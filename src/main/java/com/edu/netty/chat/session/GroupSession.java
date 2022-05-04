package com.edu.netty.chat.session;

import io.netty.channel.Channel;

import java.util.Set;

public interface GroupSession {


    /**
     * 创建一个聊天组，如果不存在，才能创建成功，否则返回 null
     * @param groupName
     * @param members
     * @return
     */
    Group createGroup(String groupName, Set<String> members);


    /**
     * 加入聊天组,
     * @param groupName
     * @param member
     * @return 如果组不存在则返回 null, 否则返回组对象
     */
    Group joinMember(String groupName, String member);

    /**
     * 移除组成员
     * @param groupName
     * @param member
     * @return 如果组不存在则返回 null, 否则返回组对象
     */
    Group removeMember(String groupName, String member);


    /**
     * 删除聊天组
     * @param groupName
     * @return 如果组不存在则返回 null, 否则返回组对象
     */
    Group removeGroup(String groupName);


    /**
     * 获取组成员
     * @param groupName
     * @return 返回组成员的集合, 没有返回空集合
     */
    Set<String> getMembers(String groupName);


    /**
     * 获取组成员的 Channel 集合
     * @param groupName
     * @return 返回组成员的 Channel 集合, 没有返回空集合
     */
    Set<Channel> getMemberChannel(String groupName);

}
