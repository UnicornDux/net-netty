package com.edu.netty.source;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 测试 系统参数与 backlog
 * -----------------------------------------------------------------------
 *  > TCP 连接中的 半连接队列(sync-queue)与 全连接队列(accept queue)的数量控制
 *  > 我们调整的时候，需要同时调整系统的参数与服务端程序的参数
 * -----------------------------------------------------------------------
 *  1. 系统参数的调整
 *      > 半连接的队列控制 (syncookies启用时，这个参数被忽略)：/proc/sys/net/ipv4/tcp_max_syn_backlog  
 *      > 全连接数量的控制 : /proc/sys/net/core/somaxconn  
 *  2. 程序参数控制
 *      > option(ChannelOption.SO_BACKLOG, value)
 * ---------------------
 *  一旦超过这个参数，程序中将会抛出异常，告诉你服务器连接已到达最大值，不接受连接
 *
 */ 

public class TestBackLog {

    public static void main(String[] args) {
        // NioServerTest();
        // BioServerTest();
    }

    public static void NioServerTest() {

        // 由于半连接队列与全连接的存在，当我们使用非阻塞的程序观测到这个现象
        // 这个过程需要阻断在 accept 方法执行之前, 断点方式
        new Thread(() -> {
            try {
               new ServerBootstrap()
                   .group(new NioEventLoopGroup())
                   // 将程序参数调整为接收两个连接 (系统参数与程序参数需要同时调整)
                   .option(ChannelOption.SO_BACKLOG, 2)
                   .channel(NioServerSocketChannel.class)
                   .childHandler(new ChannelInitializer<NioSocketChannel>(){
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new LoggingHandler());
                        }
                   }).bind(8090).sync().channel().closeFuture().sync();
            } catch(Exception e){
                e.printStackTrace();
            }
        }).start(); 

        // 启动多个客户端，观测到服务端的连接数量
        new Thread(() -> {
            try {
                new Bootstrap()
                    .group(new NioEventLoopGroup())
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>(){
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new LoggingHandler());
                        }
                    }).connect("localhost", 8090).sync().channel().closeFuture().sync();
            }catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void BioServerTest() {
        new Thread(() ->{
            try {
                // 在构建对象的时候传入 SO_BACKLOG 参数，表示允许的连接数量
                ServerSocket ss = new ServerSocket(8899, 2);
                // 由于阻塞在 accept() 中，可以轻松观测到效果
                Socket socket = ss.accept();
                System.out.println(socket);
                System.in.read();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() ->{
            try {
                Socket s = new Socket();
                System.out.println(new Date() + "connecting....");
                s.connect(new InetSocketAddress("localhost", 8899), 1000);
                System.out.println(new Date() + "connected..");
                s.getOutputStream().wait(1);
                System.in.read();
            } catch(Exception e) {
                System.out.println("connected timeout....");
                e.printStackTrace();
            }
        }).start();
    }
}
