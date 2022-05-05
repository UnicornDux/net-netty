package com.edu.netty.chat.handler;

import com.edu.netty.chat.service.UserServiceFactory;
import com.edu.netty.chat.session.SessionFactory;
import com.edu.netty.message.LoginRequestMessage;
import com.edu.netty.message.LoginResponseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class LoginRequestMessageHandler extends SimpleChannelInboundHandler<LoginRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, LoginRequestMessage loginRequestMessage) throws Exception {
        String userName = loginRequestMessage.getUserName();
        String password = loginRequestMessage.getPassword();
        boolean login = UserServiceFactory.getUserService().login(userName, password);
        LoginResponseMessage message;
        if (login) {
            SessionFactory.getSession().bind(channelHandlerContext.channel(), userName);
            message = new LoginResponseMessage(true, "登录成功");
        } else {
            message = new LoginResponseMessage(false, "登录失败");
        }
        channelHandlerContext.writeAndFlush(message);
    }
}
