package com.edu.netty;

/*
 * Netty 通讯框架的引入
 * 简单的说：Netty封装了JDK的NIO，让你用得更爽，你不用再写一大堆复杂的代码了。
 * 官方术语：Netty是一个异步事件驱动的网络应用框架，用于快速开发可维护的高性能服务器和客户端。
 *
 * 下面是使用Netty不使用JDK原生NIO的一些原因：
 *
 * 🔹 使用JDK自带的NIO需要了解太多的概念，编程复杂
 * 🔹 Netty底层IO模型随意切换，而这一切只需要做微小的改动，就可以直接从NIO模型变身为IO模型
 * 🔹 Netty自带的拆包解包，异常检测等机制，可以从NIO的繁重细节中脱离出来，只需要关心业务逻辑
 * 🔹 Netty解决了JDK的很多包括空轮询在内的bug
 * 🔹 Netty底层对线程，selector做了很多细小的优化，精心设计的线程模型做到非常高效的并发处理
 * 🔹 自带各种协议栈让你处理任何一种通用协议都几乎不用亲自动手
 * 🔹 Netty社区活跃，遇到问题随时邮件列表或者issue
 * 🔹 Netty已经历各大rpc框架，消息中间件，分布式通信中间件线上的广泛验证，健壮性无比强大
 */

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

public class NettyServer {
    public static void main(String[] args) {
        // 用于接收客户端的连接，为已经接收的连接创建子通道，一般用于服务端
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        // EventLoopGroup 包含有多个EventLoop实例，是用来管理EventLoop的组件，
        // 用来接受新连接的线程
        NioEventLoopGroup boos = new NioEventLoopGroup();
        // 读取数据的线程
        NioEventLoopGroup worker = new NioEventLoopGroup();

        try {
            // 服务端执行
            // 1. 启动器，负责组装 Netty 组件，启动服务器
            ChannelFuture future = serverBootstrap
                // 2.指定工作组 boosEventLoop, WorkEventLoop (selector, Thread)
                .group(boos,worker)
                // Channel对网络套接字的IO操作
                // 例如：读、写、连接、绑定等操作进行适配与封装的组件
                // 3. 注入服务端 ServerSocketChannel 实现
                .channel(NioServerSocketChannel.class)
                // 4. boos 负责处理连接，Worker(child) 负责处理读写, childrenHandle决定了worker(chard) 能执行哪些操作
                .childHandler(
                    // 5. channel 代表和客户端进行数据读写的通道 Initializer, 负责添加 ChannelHandler
                    // ChannelInitializer 对刚创建的channel进行初始化
                    // 将ChannelHandler添加到channel中的channelPipeline处理链路中
                    new ChannelInitializer<NioSocketChannel>() {

                    // 这个回调在连接成功后才被调用，第五步只是注册了这样一个事件，并不是立即执行
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) {
                        // 6. 添加具体 Handler
                        // 组件从流水线头部进入，流水线上的工人按顺序对组件进行加工，
                        // 流水线相当与ChannelPipeline
                        // 工人相当于 channelHandler
                        nioSocketChannel.pipeline()
                            .addLast(new StringDecoder()) // 将 ByteBuf 转换为字符串
                            .addLast(new SimpleChannelInboundHandler<String>() { // 自定义 Handler
                            // 安排工人的具体工作内容
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) {
                                System.out.println(s);
                            }
                        });
                    }
                    // 7. 监听端口
            }).bind(8000).sync();
            Channel channel = future.channel();
            channel.closeFuture().sync();
        }catch(InterruptedException e) {
            e.printStackTrace();
        }finally{
            boos.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
