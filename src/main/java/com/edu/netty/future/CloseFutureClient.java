package com.edu.netty.future;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;


/**
 * 优雅的关闭 Channel
 * --------------------------------------------------------
 *  监测到真正的关闭 Channel，并做一些善后工作, 做到程序的优雅关闭
 *  -------------------------------
 *   1. 做到关闭信号发出后，EventLoop 阻断任务的提交，
 *   2. 将已经提交的任务全部执行完成，然后停止应用
 */

@Slf4j
public class CloseFutureClient {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        ChannelFuture channelFuture = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        channel.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        channel.pipeline().addLast(new StringEncoder());
                    }
                })
                .connect("localhost", 8000);
        Channel channel = channelFuture.sync().channel();

        new Thread(() -> {
            Scanner scan = new Scanner(System.in);
            while (true) {
                String msg = scan.nextLine();
                if ("q".equals(msg)) {
                   // 异步方法, 非阻塞
                   channel.close();
                   // 这里关闭连接打印出来的时候，因为异步的原因，真正关闭
                   // 连接的是一个新的 NiOEventLoopGroup 的线程, 不是当前线程
                   // 所以channel 什么时候被真的关闭这里是无法监测到的
                   // log.debug("close channel....");
                   break;
                }
                channel.writeAndFlush(msg);
            }
        }, "input-thread").start();


        /**
         * CloseFuture 可以监测到 Channel 关闭的情况，并在关闭之后进行一些善后操作
         * -----------------------------------------------------------------
         *  1. 使用同步阻塞方法获取到 CloseFuture 关闭的结果
         *  2. 添加监听器来监测关闭结果
         */
        // ChannelFuture closeFuture = channel.closeFuture();
        // log.debug("waiting for close");
        // closeFuture.sync();
        // log.debug("post operation after close");

        channel.closeFuture().addListener(future -> {
            // 关闭之后的操作
            log.debug("post operation after close");
            // channel 关闭之后，程序并没有停止，因为 NioEventLoopGroup 的线程没有关闭
            // 程序并不会停止，这时候如何让程序最好自己的善后工后停止服务。
            // 程序优雅的停止
            group.shutdownGracefully();

        });
    }
}
