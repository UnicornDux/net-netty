package com.edu.netty.source;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;

public class TestConnectionTimeout {

    public static void main(String[] args) {

        // 客户端通过 .option() 方法配置参数, 给SocketChannel 配置参数

        // new ServerBootStrap().option()      给 ServerSocketChannel 配置参数
        // new ServerBootStrap).childOption()  给 SocketChannel 配置参数

        NioEventLoopGroup group = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(group)
                // 这里的 SO_TIMEOUT 是传统的阻塞IO 模式下调整阻塞时间的参数
                // .option(ChannelOption.SO_TIMEOUT)
                
                .channel(NioServerSocketChannel.class)
                .childHandler(new LoggingHandler());
        ChannelFuture future = bootstrap.bind(8080);
        try {
            future.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }

    // 客户端超时设置
    public static void Client(){
        NioEventLoopGroup group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                // 设置客户端连接超时时间
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler());
        ChannelFuture future = bootstrap.connect("localhost", 8080);
        try {
            future.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }
}
