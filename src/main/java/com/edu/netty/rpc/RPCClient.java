package com.edu.netty.rpc;

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
public class RPCClient {

    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGER_HANDLER = new LoggingHandler();
        SharableMessageCodec MESSAGE_CODEC = new SharableMessageCodec();

        // rpc 请求响应处理器
        RpcResponseMessageHandler RPC_RESPONSE_HANDLER = new RpcResponseMessageHandler();
        try {
            Channel channel = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                            nioSocketChannel.pipeline()
                                    .addLast(new ProtocolFrameDecoder())
                                    .addLast(LOGGER_HANDLER)
                                    .addLast(MESSAGE_CODEC)
                                    .addLast(RPC_RESPONSE_HANDLER);
                        }
                    }).connect("localhost", 8000).sync().channel();
            // 发送 Rpc 请求
            ChannelFuture future = channel.writeAndFlush(new RpcRequestMessage(
                    1,
                    "com.edu.netty.rpc.service.HelloService",
                    "sayHello",
                    String.class,
                    new Class[]{String.class},
                    new Object[]{"Alex"}
            )).addListener(promise -> {
                // 添加异步监听来处理请求失败的情况
                if (!promise.isSuccess()){
                    log.error("error:", promise.cause());
                }else {
                    promise.getNow();
                }
            });
            channel.closeFuture().sync();
        }catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }
}
