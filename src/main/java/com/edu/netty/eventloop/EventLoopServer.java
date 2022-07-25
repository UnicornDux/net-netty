package com.edu.netty.eventloop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * --------------------------------------------------------------
 *  这里介绍一些关于 EventLoop 的一些细节
 * --------------------------------------------------------------
 *  > EventLoop 是一个线程池，用于处理 IO 操作, 一个 Channel 一但与某个 EventLoop 建立连接，
 *    就会被分配到这个 EventLoop 中进行处理, 形成了一个绑定的关系，这样后续这个 channel 中的操作
 *    都会被这个 EventLoop 中的 Handler 处理这样做的主要目的是为了线程安全.
 *  > 官方建议将 EventLoop 尽可能 的细分，做到专职专用，
 *  > 我们可以为某写些耗时的操作，指定专门处理的 EventLoop，这样就可以提高效率.
 *
 * */

@Slf4j
public class EventLoopServer {
    public static void main(String[] args) {
        // 细分工作，创建独立的 EventLoopGroup, 用于处理耗时较长的特殊任务
        EventLoopGroup hGroup = new DefaultEventLoopGroup(2);
        // 此处不需要指定 boss 的个数，因为服务端程序最多使用一个Selector
        EventLoopGroup boss = new NioEventLoopGroup();
        // 声明 work 线程组，用于处理 IO 操作，比如读写文件、网络读写等等
        EventLoopGroup work = new NioEventLoopGroup(2);

        try {
            Channel channel = new ServerBootstrap()
                // boos(只负责 ServerSocketChannel 上 accept),
                // worker (read, write...)
                .group(boss, work)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                       nioSocketChannel.pipeline().addLast("handler1",new ChannelInboundHandlerAdapter(){
                           @Override
                           public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                               ByteBuf buf = msg instanceof ByteBuf ? ((ByteBuf) msg): null;
                               if (buf != null) {
                                   log.debug("接收到消息：{}",buf.toString(Charset.defaultCharset()));
                               }
                               // 将这个消息发送给下一个 ChannelInboundHandler, 自定义的需要传递，否则将不会往下传递
                               ctx.fireChannelRead(msg);
                           }
                           // 使用事先定义好的 EventLoopGroup 来处理 更加耗时的任务
                           // 这样可以将不同的任务类型分配给不同的 EventLoopGroup, 
                           // 可以有效避免某类 Handler 处理慢导致资源占用而阻塞
                       }).addLast(hGroup, "handler2", new ChannelInboundHandlerAdapter() {
                           @Override
                           public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                               ByteBuf buf = msg instanceof ByteBuf ? ((ByteBuf) msg): null;
                               if (buf != null) {
                                   TimeUnit.SECONDS.sleep(5);
                                   log.debug("接收到消息：{}",buf.toString(Charset.defaultCharset()));
                               }
                           }
                       });
                    }
                }).bind(8000).sync().channel();
            channel.closeFuture().sync();
        }catch(InterruptedException e) {
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
            hGroup.shutdownGracefully();
        }
    }
}
