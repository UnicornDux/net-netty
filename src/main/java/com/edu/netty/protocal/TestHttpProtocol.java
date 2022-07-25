package com.edu.netty.protocal;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

@Slf4j
public class TestHttpProtocol {

    public static void main(String[] args) {
        NioEventLoopGroup boss  = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            ChannelFuture sync = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));

                        // Netty 内置的 HttpServerCodec 和 HttpClientCodec 解码器
                        ch.pipeline().addLast(new HttpServerCodec());

                        // SimpleChannelInboundHandler<HttpRequest> 可以限定这种处理器只对泛型内限定的消息感兴趣，
                        // 其他类型的消息将不会被这个处理器处理
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<HttpRequest>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpRequest httpRequest) throws Exception {
                                // 获取数据
                                log.debug(httpRequest.uri());
                                log.debug(httpRequest.headers().toString());

                                // Netty 提供了 DefaultFullHttpResponse 类来构造响应
                                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                                        httpRequest.protocolVersion(),
                                        HttpResponseStatus.OK
                                );

                                // 返回内容
                                response.content().writeBytes("<h1>hello world</h1>".getBytes());

                                // 告诉浏览器响应的长度，避免浏览器一直在等待响应数据
                                response.headers().add(CONTENT_LENGTH, response.content().readableBytes());

                                // 写回到 channel 中的数据会经过出站处理器后返回到浏览器中
                                ch.writeAndFlush(response);
                            }
                        });

                        // 自定义处理器，处理 http 解码器返回的结果
                        // ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                        //     @Override
                        //     public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg) throws Exception {
                        //         // 一个 HTTP 请求发送到服务器被识别 为 HttpRequest, HttpContent 两部分
                        //         log.debug("接收数据的类型: {}", msg.getClass());
                        //         if (msg instanceof HttpRequest) {
                        //             // 处理 HttpRequest 
                        //         } else if (msg instanceof HttpContent) {
                        //            // 处理 HttpContent
                        //         }
                        //     }
                        // });
                    }
                }).bind(8000).sync();
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
