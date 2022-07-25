package com.edu.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AioNetServer {
    // 异步 IO 服务器端程序
    public static void main(String[] args) {
        try {
            AsynchronousServerSocketChannel ssc = AsynchronousServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(8899));
            ssc.accept(null, new AcceptHandler(ssc));
            // 由于异步的时候，服务器中真正处理各种事件的是后台线程
            // 所以主线程不糊阻塞，因此需要将主线程在这里阻塞， 否则程序随着主线程结束而停止了
            System.in.read();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private static void closeChannel(AsynchronousSocketChannel sc) {
        try {
            log.debug("{} : {} close", Thread.currentThread().getName(), sc.getRemoteAddress());
            sc.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    // readable 事件的处理器
    private static class ReadHandler implements CompletionHandler<Integer, ByteBuffer> {

        private final AsynchronousSocketChannel sc;

        public ReadHandler(AsynchronousSocketChannel sc) {
            this.sc = sc;
        }
        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            try {
                if (result == -1) {
                    closeChannel(sc);
                    return;
                }
                log.debug("{} : {}", Thread.currentThread().getName(), sc.getRemoteAddress());
                attachment.flip();
                log.debug("receive msg: {}", Charset.defaultCharset().decode(attachment));
                attachment.clear();
                // 处理完一个 read 时，需要再次调用 read 方法来处理下一个 read 事件
                sc.read(attachment, attachment, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            closeChannel(sc);
            exc.printStackTrace();
        }
    }

    // writable 事件写处理器
    private static class WriteHandler implements CompletionHandler<Integer, ByteBuffer> {

        private final AsynchronousSocketChannel sc;

        public WriteHandler(AsynchronousSocketChannel sc) {
            this.sc = sc;
        }
        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            // 如果作为附件的 buffer 还有内容，需要再次 write 写出剩余的内容
            if (attachment.hasRemaining()) {
                sc.write(attachment);
            }
        }
        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            exc.printStackTrace();
            closeChannel(sc);
        }
    }

    // ServerSocketChannel 需要执行的 accept 事件处理的处理器，
    // 这个处理器需要实现 CompletionHandler, 成功回调函数, 失败的回调函数
    public static class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        private final AsynchronousServerSocketChannel ssc;


        // 构造函数创建时注入服务端的 ServerSocketChannel 
        public AcceptHandler(AsynchronousServerSocketChannel ssc) {
            this.ssc = ssc;
        }
        // accept 被成功执行的时候触发的回调函数
        @Override
        public void completed(AsynchronousSocketChannel sc, Object attachment) {
            try {
                // 显示客户端连接的信息
                log.debug("{}:{} connected", Thread.currentThread().getName(), sc.getRemoteAddress());
            } catch(Exception e){
                e.printStackTrace();
            }
            ByteBuffer buffer = ByteBuffer.allocate(16);
            // 读事件有 ReadHandler 处理器来处理
            sc.read(buffer, buffer, new ReadHandler(sc));
            // 写事件由 WriteHandler 处理器处理
            sc.write(
                Charset.defaultCharset().encode("server hello"),
                ByteBuffer.allocate(16), 
                new WriteHandler(sc)
            );
            // 这里表示，处理好一个 accept 事件后，需要再次调用一下 accept 来处理下一个 accept 事件
            ssc.accept(null, this);
        }

        // accept 处理失败触发的回调函数
        @Override
        public void failed(Throwable exc, Object attachment) {
            // 失败的时候，这个 异常会作为参数传入，方便处理异常，同时附件也会被作为参数传入
            // 方便做一些善后的工作，比如资源的释放等
            attachment = null;
            exc.printStackTrace();
        }
    }
} 
