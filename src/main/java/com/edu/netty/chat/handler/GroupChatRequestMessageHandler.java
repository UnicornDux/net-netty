package com.edu.netty.chat.handler;

import com.edu.netty.chat.session.GroupSession;
import com.edu.netty.chat.session.GroupSessionFactory;
import com.edu.netty.message.GroupChatRequestMessage;
import com.edu.netty.message.GroupChatResponseMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;

@ChannelHandler.Sharable
public class GroupChatRequestMessageHandler extends SimpleChannelInboundHandler<GroupChatRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupChatRequestMessage msg) throws Exception {

        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        List<Channel> memberChannel = groupSession.getMemberChannel(msg.getGroupName());

        memberChannel.forEach(channel -> {
            if (channel != ctx.channel()) {
                channel.writeAndFlush(new GroupChatResponseMessage(msg.getFrom(), msg.getGroupName(), msg.getContent()));
            }
        });
    }
}
