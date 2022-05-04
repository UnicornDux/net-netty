package com.edu.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class NIOWriteServer {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new java.net.InetSocketAddress(8000));
        serverSocketChannel.configureBlocking(false);
        Selector selector = Selector.open();

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();

            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                try {
                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = ((ServerSocketChannel)key.channel()).accept();
                        socketChannel.configureBlocking(false);
                        // 注册到 Selector
                        SelectionKey seKey = socketChannel.register(selector, 0, null);
                        seKey.interestOps(SelectionKey.OP_READ);

                        // 服务端向客户端发送大量数据
                        String s = Stream.generate(() -> "o").limit(3000000).collect(Collectors.toList()).toString();

                        //log.debug(s);
                        ByteBuffer buffer = Charset.defaultCharset().encode(s);

                        /*
                         * 这里的思路是: 先发送一次，然后，如果数据还有的话，可以为这个 selectedKey 添加一个 OP_WRITE 事件
                         * 如此一来，线程可以释放了去处理其他客户端业务，而不必一直在这里阻塞，
                         * 等到事件处理下一次轮询的时候接着处理写入数据的操作。
                         */

                        // 返回值代表实际写入的数据
                        int num = socketChannel.write(buffer);
                        log.debug("写入数据: {}", num);

                        if (buffer.hasRemaining()) {
                            // 添加一种关注事件
                            seKey.interestOps(seKey.interestOps() + SelectionKey.OP_WRITE);
                            // 这是位运算，把 OP_WRITE 添加到 interestOps
                            // seKey.interestOps(seKey.interestOps() | SelectionKey.OP_WRITE);

                            // 将当前没有写完的数据附着在selectedKey上
                            seKey.attach(buffer);
                        }

                        /*
                        // 数据写出，数据量大的时候，可能需要一直再写，这样会阻塞其他的客户端运行, 同时缓冲区由于网络等原因发送较慢
                        // 而程序一直阻塞在这里循环操作，还阻塞了其他客户端的运行
                        while (encode.hasRemaining()) {
                            // 返回值代表实际写入的数据
                            int num = accept.write(encode);
                            log.debug("写入数据: {}", num);
                        }
                        */
                    }else if (key.isWritable()) {
                        SocketChannel socketChannel = (SocketChannel)key.channel();
                        ByteBuffer buffer = (ByteBuffer)key.attachment();
                        int write = socketChannel.write(buffer);
                        log.debug("写入数据: {}", write);
                        if (!buffer.hasRemaining()) {
                            // 这个挂载的 ByteBuffer 是很大的，不再有数据的时候可以将附件移除，这样就可以垃圾回收了
                            key.attach(null);
                            // 不再关注 OP_WRITE 事件
                            key.interestOps(key.interestOps() - SelectionKey.OP_WRITE);
                        }

                    }
                }catch (Exception e) {
                    e.printStackTrace();
                    key.cancel();
                }finally {
                    iterator.remove();
                }
            }
        }
    }
}
