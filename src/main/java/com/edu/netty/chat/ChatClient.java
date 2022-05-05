package com.edu.netty.chat;


import com.edu.netty.chat.protocal.ProtocolFrameDecoder;
import com.edu.netty.message.*;
import com.edu.netty.protocal.SharableMessageCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ChatClient {

    public static final SharableMessageCodec SHARABLE_MESSAGE_CODEC = new SharableMessageCodec();
    public static final LoggingHandler LOGGING_HANDLER = new LoggingHandler();

    // 可以用在多线程通信, 由于处理用户输入信息与发送登录请求的线程是不同的，所以需要相互之间通信
    // 交换状态，决定程序运行逻辑
    public static final CountDownLatch WAIT_LOGIN = new CountDownLatch(1);
    public static final AtomicBoolean LOGIN = new AtomicBoolean(false);


    public static void main(String[] args) {

        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                            .addLast(new ProtocolFrameDecoder())
                            // 调试日志，调试完成后可关闭
                            // .addLast(LOGGING_HANDLER)
                            .addLast(SHARABLE_MESSAGE_CODEC)
                            .addLast(new IdleStateHandler(0, 3, 0))
                                .addLast(new ChannelDuplexHandler(){
                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                        IdleStateEvent event = (IdleStateEvent) evt;
                                        if (event.state() == IdleState.WRITER_IDLE) {
                                            // log.info("写空闲超时,触发回调事件");
                                            ctx.writeAndFlush(new PingMessage());
                                        }
                                    }
                                })
                            .addLast("client_handler", new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    log.debug("收到响应: {}", msg);
                                    handlerServerResponse(ctx, msg);
                                }

                                // 连接建立后开始进行接收用户输入的登录
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    startChatSystem(ctx);
                                }
                            });
                    }
                });
            ChannelFuture channelFuture = bootstrap.connect("localhost", 8000).sync();
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
           group.shutdownGracefully();
        }
    }

    private static void startChatSystem(ChannelHandlerContext ctx) {
        // 负责接收用户在控制台的输入，负责向服务器发送各种数据，
        // 这里另起一个线程去操作这些，否则直接使用 NioEventGroup 中的线程
        // 而用户操作是一个耗时阻塞的过程，不能长事件占用 NioEventGroup 中的线程
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("请输入用户名:");
            String username = scanner.nextLine();
            System.out.println("请输入密码:");
            String password = scanner.nextLine();

            // 构造消息
            LoginRequestMessage loginRequestMessage = new LoginRequestMessage(username,password,"");
            ctx.writeAndFlush(loginRequestMessage);

            // 阻塞, 使得用户可以持续交互，不会停止
            try {
                WAIT_LOGIN.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 登录失败
            if (!LOGIN.get()) {
                ctx.channel().close();
                return;
            }
            // 登录成功之后，继续交互
            while(true) {
                System.out.println("========================================");
                System.out.println("> send     [username]  [message]");
                System.out.println("> gsend    [groupName] [message]");
                System.out.println("> gcreate  [groupName] [m1,m2,m3...]");
                System.out.println("> gmembers [groupName]");
                System.out.println("> gjoin    [groupName]");
                System.out.println("> gquit    [groupName]");
                System.out.println("> quit");
                System.out.println("========================================");
                String command = scanner.nextLine();
                String[] args = command.split(" ");
                switch (args[0]) {
                    case "send":
                        ctx.writeAndFlush(new ChatRequestMessage(username, args[1], args[2]));
                        break;
                    case "gsend":
                        ctx.writeAndFlush(new GroupChatRequestMessage(username, args[1], args[2]));
                        break;
                    case "gcreate":
                        Set<String> set = new HashSet<>(Arrays.asList(args[2].split(",")));
                        set.add(username);
                        ctx.writeAndFlush(new GroupCreateRequestMessage( args[1], set));
                        break;
                    case "gmembers":
                        ctx.writeAndFlush(new GroupMemberRequestMessage(args[1]));
                        break;
                    case "gjoin":
                        ctx.writeAndFlush(new GroupJoinRequestMessage(args[1], username));
                        break;
                    case "gquit":
                        ctx.writeAndFlush(new GroupQuitRequestMessage(username, args[1]));
                        break;
                    case "quit":
                        // 服务端可以监测到 Channel 的关闭，关闭之后做一些善后操作。
                        ctx.channel().close();
                        return;
                }
            }
        }, "user_input").start();
    }

    private static void handlerServerResponse(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof LoginResponseMessage) {
            LoginResponseMessage response = (LoginResponseMessage) msg;
            if (response.isSuccess()) {
                LOGIN.set(true);
                System.out.println(String.format("[%s] :: ---- 登录成功，欢迎!!----", "Success"));
            }
            WAIT_LOGIN.countDown();
        }else if (msg instanceof ChatResponseMessage) {
            ChatResponseMessage response = (ChatResponseMessage) msg;
            if (response.isSuccess()) {
                System.out.println(String.format("%s : > %s", response.getFrom(), response.getContent()));
            }else {
                System.out.println(String.format("[%s] :: ---- %s ----", "Waring", response.getReason()));
            }
        } else if (msg instanceof GroupCreateResponseMessage) {
            GroupCreateResponseMessage response = (GroupCreateResponseMessage) msg;
            if(response.isSuccess()) {
                System.out.println(String.format("[%s] :: ---- %s ----", "Success", response.getReason()));
            }else {
                System.out.println(String.format("[%s] :: ---- %s ----", "Waring", response.getReason()));
            }
        }else if (msg instanceof GroupChatResponseMessage) {
            GroupChatResponseMessage response = (GroupChatResponseMessage) msg;
            if (response.isSuccess()) {
                System.out.println(
                        String.format(
                                "[%s::%s] : > %s",
                                response.getGroupName(),
                                response.getFrom(),
                                response.getContent()
                        )
                );
            }else{
                System.out.println(String.format("[%s] :: ---- %s ----", "Waring", response.getReason()));
            }
        }
    }
}
