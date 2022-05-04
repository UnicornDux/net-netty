package com.edu.netty;

import io.netty.bootstrap.Bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

/* 客户端示例 */
public class NettyClient {
    public static void main(String[] args) throws InterruptedException {
        // 不接受新的连接， 并且在父类通道类中完成一些操作，一般用于客户端
        Bootstrap boos = new Bootstrap();
        // EventLoopGroup 包含多个eventLoop实例，用来管理eventLoop
        NioEventLoopGroup group = new NioEventLoopGroup();

        // 1. 启动客户端执行
        boos
            // 2. 添加处理组
            .group(group)
            // 3. 选择客户端 Channel 实现
            // channel对网络套接字的I/O操作
            .channel(NioSocketChannel.class)
            // 4. channelInitializer 对刚创建的channel进行初始化
            // 将channelHandler 添加到channel 中的channelPipeline处理链路中
            .handler(new ChannelInitializer<Channel>() {
                // 连接成功后被调用
                @Override
                protected void initChannel(Channel channel) {
                    // 组件从流水线头部进入，工人按照一定的顺序对组件进行加工
                    // 流水线是相当于 channelPipeline
                    // 工人相当于 channelHandler
                    channel.pipeline().addLast(new StringEncoder());
                }
            });
        Channel channel = boos
                // 5. 客户端连接服务端
                .connect("127.0.0.1", 8000)
                // 阻塞方法，直到连接建立
                .sync()
                // 连接成功，获取channel
                .channel();

        while (true){
            // 客户端使用writerAndFlush 方法向服务端发送数据，返回的是channelFuture
            // 与JDK中的Future卡接口类似，即实现并行处理的效果
            // 可以在操作成功或失败时可以自动触发监听器中的事件处理方法
            ChannelFuture future = channel.writeAndFlush("测试数据");
            Thread.sleep(2000);
        }
    }
}
