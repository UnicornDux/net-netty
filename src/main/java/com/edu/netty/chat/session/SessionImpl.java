package com.edu.netty.chat.session;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionImpl implements Session {

    private final Map<String, Channel> userNameChannelMap = new ConcurrentHashMap<>(16);
    private final Map<Channel, String> channelUserNameMap = new ConcurrentHashMap<>(16);
    private final Map<Channel, Map<String,Object>> channelAttributeMap = new ConcurrentHashMap<>(16);


    @Override
    public void bind(Channel channel, String name) {
        userNameChannelMap.put(name, channel);
        channelUserNameMap.put(channel, name);
        channelAttributeMap.put(channel, new ConcurrentHashMap<>(16));
    }

    @Override
    public void unbind(Channel channel) {
        String name = channelUserNameMap.remove(channel);
        userNameChannelMap.remove(name);
        channelAttributeMap.remove(channel);
    }

    @Override
    public Object getAttribute(Channel channel, String name) {
        return channelAttributeMap.get(channel).get(name);
    }

    @Override
    public void setAttribute(Channel channel, String name, Object value) {
        channelAttributeMap.get(channel).put(name, value);

    }

    @Override
    public Channel getChannel(String name) {
        return userNameChannelMap.get(name);
    }
}
