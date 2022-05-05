package com.edu.netty.chat.handler;

import com.edu.netty.chat.session.Group;
import com.edu.netty.chat.session.GroupSession;
import com.edu.netty.chat.session.GroupSessionFactory;
import com.edu.netty.message.GroupCreateRequestMessage;
import com.edu.netty.message.GroupCreateResponseMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;
import java.util.Set;

@ChannelHandler.Sharable
public class GroupCreateRequestMessageHandler extends SimpleChannelInboundHandler<GroupCreateRequestMessage> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupCreateRequestMessage msg) throws Exception {
        
        String groupName = msg.getGroupName();
        Set<String> members = msg.getMembers();
        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        Group group = groupSession.createGroup(groupName, members);

        if (group == null) {
            ctx.channel().writeAndFlush(new GroupCreateResponseMessage(true,"创建成功"));
            // 创建成功后，需要给每个群组成员发消息告诉他们已经加入了群组
            List<Channel> memberChannel = groupSession.getMemberChannel(groupName);
            memberChannel.forEach(channel -> {
                if (channel != ctx.channel()) {
                    channel.writeAndFlush(new GroupCreateResponseMessage(true,"您已被拉入"+groupName+"群聊"));
                }
            });
        }
        // group 为 null 的时候表示要创建的群组已经存在
        // 直接告知需要创建群的客户端已经存在即可
        else{
            ctx.channel().writeAndFlush(new GroupCreateResponseMessage(false, "群组已经存在"));
        }
    }
}
