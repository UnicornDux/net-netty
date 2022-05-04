package com.edu.netty.sticky_splicing;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Random;
import java.util.stream.Stream;

public class FixedLengthDecoderClient {


    public static void main(String[] args) {
        // fill_dash_to_ten('0', 10);
        //
        send_msg();
    }

    public static void send_msg(){
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup());
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        ByteBuf byteBuf = ctx.alloc().buffer();
                        char c = '0';
                        Stream.generate(() -> new Random().nextInt(10) + 1).limit(10).forEach(s -> {
                                byte[] bytes = fill_dash_to_ten(c,s);
                                byteBuf.writeBytes(bytes);
                        });
                        ctx.writeAndFlush(byteBuf);
                    }
                });
            }
        });
        ChannelFuture channelFuture = bootstrap.connect("localhost", 8000);
        try {
            Channel channel = channelFuture.sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            bootstrap.group().shutdownGracefully();
        }
    }

    /**
     * 拼接定长消息
     * @param c
     * @param s
     * @return
     */
    private static byte[] fill_dash_to_ten(char c, Integer s) {
        if (s < 0){
            s = 0;
        }
        Character[] characters = Stream.generate(() -> c).limit(s).toArray(Character[]::new);
        byte[] bytes = new byte[10];
        for (int i = 0; i < 10; i++) {
            if (i < characters.length) {
                bytes[i] = (byte) characters[i].charValue();
            }else{
                bytes[i] = '-';
            }
        }
        return bytes;
    }
}
