package com.edu.netty.chat.client;

import com.edu.netty.chat.protocal.ProtocolFrameDecoder;
import com.edu.netty.message.RpcRequestMessage;
import com.edu.netty.protocal.SharableMessageCodec;
import com.edu.netty.rpc.handler.RpcResponseMessageHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcClient {

    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGER_HANDLER = new LoggingHandler();
        SharableMessageCodec MESSAGE_CODEC = new SharableMessageCodec();
        RpcResponseMessageHandler RPC_RESPONSE_HANDLER = new RpcResponseMessageHandler();

        ChannelFuture channelFuture = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast(new ProtocolFrameDecoder())
                        .addLast(LOGGER_HANDLER)
                        .addLast(MESSAGE_CODEC)
                        .addLast(RPC_RESPONSE_HANDLER);
                }
            }).connect("localhost", 8080);
        try {
            Channel channel = channelFuture.sync().channel();

            channel.writeAndFlush(new RpcRequestMessage(
                1,
                "com.edu.netty.rpc.service.HelloService",
                "sayHello",
                String.class,
                new Class[]{String.class},
                new Object[]{"Alex"}
            )).addListener( promise -> {
                // 监听服务端返回的结果
                if (promise.isSuccess()) {
                    promise.getNow();
                } else {
                   log.debug("ewquest failure :: {}", promise.cause());
                }
            });
            channel.closeFuture().sync();
        }catch(InterruptedException e){
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }
}
