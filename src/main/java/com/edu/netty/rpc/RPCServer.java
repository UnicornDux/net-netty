package com.edu.netty.rpc;

import com.edu.netty.chat.protocal.ProtocolFrameDecoder;
import com.edu.netty.protocal.SharableMessageCodec;
import com.edu.netty.rpc.handler.RpcRequestMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;

public class RPCServer {

    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        LoggingHandler LOGGER_HANDLER = new LoggingHandler();
        SharableMessageCodec MESSAGE_CODEC = new SharableMessageCodec();

        // 请求消息处理器
        RpcRequestMessageHandler RPC_REQUEST_HANDLER = new RpcRequestMessageHandler();

        try {
            ChannelFuture channelFuture = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline()
                            .addLast(new ProtocolFrameDecoder())
                            .addLast(LOGGER_HANDLER)
                            .addLast(MESSAGE_CODEC)
                            .addLast(RPC_REQUEST_HANDLER);

                    }
                }).bind(8000).sync();
            // 检测 Close 关闭的事件
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
