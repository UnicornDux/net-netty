package com.edu.netty.sticky_splicing;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

@Slf4j
public class StickySplicingClient {

    public static void main(String[] args) {

        // 这是连续发送十条消息，每次发送 16 个字节，用于观测粘包的问题
        // send_ten_message();

       /*
        *  1. 短链接的方式解决粘包问题
        * --------------------------------------------------------------------------------------------------------
        * 这是采用短链接的方式，来控制粘包的问题,
        * 每次发送完消息之后与服务端断开，然后再重新连接发送消息
        * --------------------------------------------------------------------------------------------------------
        * > 问题: 这种方式可以控制不会发生粘包的情况，但是不能解决分包的问题
        */
        Stream.generate(() -> " ").limit(10).forEach(e -> {
            log.debug("----------------------------------------------------");
            send_one_message();
        });

    }

    private static void send_ten_message() {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(worker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {

                            // channelActive 方法在连接建立成功后执行
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                // 在这里主要是演示了粘包的问题，客户端发送了十次数据，每次发送了 16 个字节
                                // 在服务端不做调整的时候，看数据是怎样接收的。
                                Stream.generate(() -> "0123456789abcdefghijklmnopqrstuvwxyz").limit(10).forEach(s -> {
                                    ByteBuf buff = ctx.alloc().buffer(36);
                                    buff.writeBytes(s.getBytes());
                                    ctx.writeAndFlush(buff);
                                });
                            }
                        });
                    }
                });
        try {
            bootstrap.connect("localhost", 8000).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            worker.shutdownGracefully();
        }
    }

    private static void send_one_message() {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(worker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {

                            // channelActive 方法在连接建立成功后执行
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                // 在这里主要是演示了粘包的问题，客户端发送了十次数据，每次发送了 16 个字节
                                // 在服务端不做调整的时候，看数据是怎样接收的。
                                Stream.generate(() -> "0123456789abcdefghijklmnopqrstuvwxyz").limit(1).forEach(s -> {
                                    ByteBuf buff = ctx.alloc().buffer(36);
                                    buff.writeBytes(s.getBytes());
                                    ctx.writeAndFlush(buff);
                                });
                            }
                        });
                    }
                });
        try {
            bootstrap.connect("localhost", 8000).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            worker.shutdownGracefully();
        }
    }
}
