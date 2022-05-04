package com.edu.netty.eventloop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EventLoopServer {
    public static void main(String[] args) {
        // 细分工作，创建独立的 EventLoopGroup, 用于处理耗时较长的特殊任务
        EventLoopGroup hGroup = new DefaultEventLoopGroup(2);
        new ServerBootstrap()
                // boos(只负责 ServerSocketChannel 上 accept),
                // worker (read, write...)
            .group(new NioEventLoopGroup(1), new NioEventLoopGroup(2))
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                   nioSocketChannel.pipeline().addLast("handler1",new ChannelInboundHandlerAdapter(){
                       @Override
                       public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                           ByteBuf buf = msg instanceof ByteBuf ? ((ByteBuf) msg): null;
                           if (buf != null) {
                               log.debug("接收到消息：{}",buf.toString(Charset.defaultCharset()));
                               // 将这个消息发送给下一个 ChannelInboundHandler, 自定义的需要传递，否则将不会往下传递
                           }
                           ctx.fireChannelRead(msg);
                       }
                       // 使用事先定义好的 EventLoopGroup 来处理 更加耗时的任务
                       // 这样可以将不同的任务类型分配给不同的 EventLoopGroup, 可以有效避免某类 Handler 处理慢导致资源占用而阻塞
                   }).addLast(hGroup, "handler2", new ChannelInboundHandlerAdapter() {
                       @Override
                       public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                           ByteBuf buf = msg instanceof ByteBuf ? ((ByteBuf) msg): null;
                           if (buf != null) {
                               TimeUnit.SECONDS.sleep(5);
                               log.debug("接收到消息：{}",buf.toString(Charset.defaultCharset()));
                           }
                       }
                   });
                }
            }).bind(8000);
    }
}
