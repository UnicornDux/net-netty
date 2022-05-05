package com.edu.netty.chat.handler;

import com.edu.netty.chat.session.SessionFactory;
import com.edu.netty.message.ChatRequestMessage;
import com.edu.netty.message.ChatResponseMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ChatRequestMessageHandler extends SimpleChannelInboundHandler<ChatRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ChatRequestMessage chatRequestMessage) throws Exception {
        String to = chatRequestMessage.getTo();
        Channel channel = SessionFactory.getSession().getChannel(to);
        // 用户在线
        if (channel != null) {
            log.debug("找到用户，发送消息");
            channel.writeAndFlush(new ChatResponseMessage(chatRequestMessage.getFrom(), chatRequestMessage.getContent()));
        }
        // 接收的用户不在线
        else {
            log.debug("没找到用户，自己返回消息");
            channelHandlerContext.channel().writeAndFlush(new ChatResponseMessage(false, "对方不在线"));
        }
    }
}
