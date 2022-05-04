package com.edu.netty.sticky_splicing;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StickySplicingServer {

    public static void main(String[] args) {

        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            // 这里主要观测数据接收的情况
            // 正常接收，可以观测到当客户端多次发送小量数据的时候，服务端接收到的数据是怎样的，
            // 调小接收缓冲区，可以观测到当客户端发送大量数据的时候，服务端接收到的数据是怎样的
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker);
            bootstrap.channel(NioServerSocketChannel.class);

            // 将接收数据的缓冲区调小，限制每次接收数据的量, 这里调整的是全局的接收缓冲区
            // bootstrap.option(ChannelOption.SO_RCVBUF, 10);

            // 调整 netty 用于接收数据的缓冲区大小，最小设置为16，不能更小
            bootstrap.childOption(
                    ChannelOption.RCVBUF_ALLOCATOR,
                    new AdaptiveRecvByteBufAllocator(16,16,16)
            );

            bootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                            nioSocketChannel.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        }
                    });
            ChannelFuture sync = bootstrap.bind(8000).sync();
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
