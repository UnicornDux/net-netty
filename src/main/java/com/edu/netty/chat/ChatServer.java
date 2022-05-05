package com.edu.netty.chat;

import com.edu.netty.chat.handler.*;
import com.edu.netty.chat.protocal.ProtocolFrameDecoder;
import com.edu.netty.protocal.SharableMessageCodec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatServer {

    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup(2);
        LoggingHandler LOGGER_HANDLE = new LoggingHandler();
        SharableMessageCodec SHARABLE_MESSAGE_CODER = new SharableMessageCodec();
        LoginRequestMessageHandler LOGIN_HANDLER = new LoginRequestMessageHandler();
        ChatRequestMessageHandler CHAT_HANDLER = new ChatRequestMessageHandler();
        GroupCreateRequestMessageHandler GROUP_CREATE = new GroupCreateRequestMessageHandler();
        GroupChatRequestMessageHandler GROUP_CHAT = new GroupChatRequestMessageHandler();
        QuitHandler QUIT_HANDLER = new QuitHandler();

        try {
            ChannelFuture channelFuture = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast(
                                // 将这些与业务协议相关的参数用一个自定义类封装起来。
                                // new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 0);
                                new ProtocolFrameDecoder()
                        )
                        .addLast(LOGGER_HANDLE)
                        .addLast(SHARABLE_MESSAGE_CODER)

                        // 监测连接是否假死(由于公网网络不稳定造成) -- 假死会造成连接占用资源但是不能释放
                        // 服务器通过监测 读写空闲时间 来判断是否假死，如果超过一定时间没有收到客户端的数据，则认为客户端已经假死
                        // 这时候这个处理器可以触发相应的事件，做一些资源的处理
                        .addLast(new IdleStateHandler(5, 0, 10))
                        // 可以同时作为 出站/入站的消息处理器
                        .addLast(new ChannelDuplexHandler(){
                            // userEventTriggered()方法是在收到由 IdleStateHandler 注册的 IdleStateEvent 事件时被调用的
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                IdleStateEvent event = (IdleStateEvent) evt;
                                if(event.state() == IdleState.READER_IDLE){
                                    log.debug("读空闲时间超时 5 秒没有接收到数据");
                                    ctx.channel().close();
                                }
                            }
                        })
                        .addLast(LOGIN_HANDLER)
                        .addLast(CHAT_HANDLER)
                        .addLast(GROUP_CREATE)
                        .addLast(GROUP_CHAT)
                        .addLast(QUIT_HANDLER);
                    }
                }).bind(8000).sync();
            // 阻塞关闭方法
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
