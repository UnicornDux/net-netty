package com.edu.netty.future;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

/* 客户端示例 */
@Slf4j
public class ChannelFutureClient {
    public static void main(String[] args) throws InterruptedException {

        // 调用 connect() 方法后得到一个 ChannelFuture 对象
        ChannelFuture channelFuture = new Bootstrap()
            .group(new NioEventLoopGroup())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    channel.pipeline().addLast(new StringEncoder());
                }
            })
            // connect() 是一个异步非阻塞的方法，这个方法会在连接请求发出后直接返回，
            // 这时候连接并没有真正建立，此时需要 Sync() 方法阻塞等待连接建立完成。
            .connect("127.0.0.1", 8000);

        // 1. [ChannelFuture] 中正确获取 channel 的方式
        // 如果这里不阻塞等待结果，直接获取 channel，这时候的 channel 没有真正获取到连接
        // 程序将不能正确执行.
        // Channel channel = channelFuture
        //     .sync()
        //     .channel();
        // log.debug("客户端: {}", channel);


        // 2. [ChannelFuture] 中正确获取 channel 的方式
        // ------------------------------------------------
        //  添加 addListener() 回调，建立连接成功后会调用这个回调
        //  返回对应的 ChannelFuture 对象，可以通过这个对象获取到 channel
        channelFuture.addListener((ChannelFutureListener) future -> {
            Channel channel = future.channel();
            log.info("channel: {}", channel);
            channel.writeAndFlush("hello world");
        });

        log.info("客户端: 发送消息");
    }
}
