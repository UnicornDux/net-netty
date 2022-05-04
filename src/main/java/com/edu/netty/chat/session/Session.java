package com.edu.netty.chat.session;

import io.netty.channel.Channel;

public interface Session {

    /**
     * 绑定会话与用户的关系
     * @param channel
     * @param name
     */
    void bind(Channel channel, String name);


    /**
     * 解绑定会话与用户的关系
     * @param channel
     */
    void unbind(Channel channel);

    /**
     * 获取属性
     * @param channel
     * @param name
     * @return
     */
    Object getAttribute(Channel channel, String name);

    /**
     * 设置属性
     * @param channel
     * @param name
     * @param value
     */
    void setAttribute(Channel channel, String name, Object value);


    /**
     * 根据用户来获取 Channel
     * @param name
     * @return
     */
    Channel getChannel(String name);

}
