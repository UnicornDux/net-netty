package com.edu.netty.protocal;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestWebsocketProtocol {
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

                            // POST body  handle
                            // add handle to handle http body request params
                            // need a args to point out the max length of the body content
                            ch.pipeline().addLast(new HttpObjectAggregator(65535));

                            // add handle to code websocket protocol ( with endpoint /ws )
                            // 1. http upgrade to websocket
                            // 2. shake hand
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));

                            ch.pipeline().addLast(new SimpleChannelInboundHandler<WebSocketFrame>() {

                                /**
                                 * new connection trigger this function,
                                 *  > store the connection,
                                 *  > ctx can follow all channel(connection) id
                                 * @param ctx
                                 * @throws Exception
                                 */
                                @Override
                                public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

                                    // use a map to store channel id and channel
                                    // then can send message to already connected session

                                    // map.put(ctx.channel().id().asLongText(), ctx.channel);
                                    log.info("{}", ctx.channel().id());
                                }

                                @Override
                                protected void channelRead0(
                                        ChannelHandlerContext channelHandlerContext, WebSocketFrame msg
                                ) throws Exception {

                                    if (msg instanceof TextWebSocketFrame) {
                                        log.info("websocket msg: {}", ((TextWebSocketFrame) msg).text());
                                    }
                                }
                            });
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
