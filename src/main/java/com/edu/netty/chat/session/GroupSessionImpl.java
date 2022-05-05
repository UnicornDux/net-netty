package com.edu.netty.chat.session;

import io.netty.channel.Channel;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GroupSessionImpl implements GroupSession {


    // 存储群组信息
    private final Map<String, Group> groupMap = new ConcurrentHashMap<>();

    public Group createGroup(String groupName, Set<String> members) {
        Group group = new Group(groupName, members);
        /**
         * ConcurrentHashMap 的 putIfAbsent方法，如果key对应的value不存在，则添加，返回 null
         * 否则返回原来的 value
         */
        return groupMap.putIfAbsent(groupName, group);
    }

    @Override
    public Group joinMember(String groupName, String member) {
        return groupMap.computeIfPresent(groupName, (key, value) -> {
            value.getMembers().add(member);
            return value;
        });
    }

    @Override
    public Group removeMember(String groupName, String member) {
        return groupMap.computeIfPresent(groupName, (key, value) -> {
            value.getMembers().remove(member);
            return value;
        });
    }

    @Override
    public Group removeGroup(String groupName) {
        return groupMap.remove(groupName);
    }

    @Override
    public Set<String> getMembers(String groupName) {
        return groupMap.getOrDefault(groupName, Group.EMPTY_GROUP).getMembers();
    }

    @Override
    public List<Channel> getMemberChannel(String groupName) {
        return getMembers(groupName).stream()
                .map(member -> SessionFactory.getSession().getChannel(member))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
