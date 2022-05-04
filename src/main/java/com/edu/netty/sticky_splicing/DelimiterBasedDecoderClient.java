package com.edu.netty.sticky_splicing;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Random;
import java.util.stream.Stream;

public class DelimiterBasedDecoderClient {
    public static void main(String[] args) {
        send_msg();
    }

    private static void send_msg() {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            ChannelFuture sync = new Bootstrap()
                .group(worker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        nioSocketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            char i = '0';
                            Random r = new Random();
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                ByteBuf buffer = ctx.alloc().buffer();
                                Stream.generate(() -> makeString(i++, r.nextInt(20) + 1))
                                    .limit(10)
                                    .forEach(s -> {
                                        buffer.writeBytes(s.toString().getBytes());
                                });
                                ctx.writeAndFlush(buffer);
                            }
                        });
                    }
                })
                .connect("localhost", 8000).sync();
                sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            worker.shutdownGracefully();
        }

    }


    /**
     * 在字符数组后添加一个回车换行符号
     *
     * @param c
     * @param count
     * @return
     */
    public static StringBuilder makeString(char c, int count) {
        StringBuilder sb = new StringBuilder(count + 2);
        Stream.generate(() -> c).limit(count).forEach(sb::append);
        sb.append("\n");
        return sb;
    }
}



