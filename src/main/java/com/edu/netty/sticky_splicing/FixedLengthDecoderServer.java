package com.edu.netty.sticky_splicing;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedLengthDecoderServer {

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
                            /**
                             * FixedLengthFrameDecoder (固定长度解码器)
                             * -------------------------------------------------------------------------
                             *  这里需要将这个定长解码器定义在消息处理器之前,在消息处理器之后就无法启到作用，
                             * > 此时无论消息缓冲区如何发送消息，粘包，还是半包，都会被定长解析器按照固定的长度进行解析出来
                             * > 粘包将会被拆分，半包将会等待收到后续字节才往下传递
                             * -------------------------------------------------------------------------
                             * > [问题] ：
                             *   - 1.消息定义的长度定义过长，将会浪费大量的空间
                             *   - 2.消息定义的过短，对于某些消息有不够使用
                             */
                            nioSocketChannel.pipeline().addLast(new FixedLengthFrameDecoder(10));
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
