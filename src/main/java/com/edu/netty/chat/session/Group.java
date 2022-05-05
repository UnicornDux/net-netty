package com.edu.netty.chat.session;

import lombok.Data;

import java.util.Collections;
import java.util.Set;


/**
 * 聊天群组。聊天室
 */
@Data
public class Group {

    /**
     * 聊天室名称
     */
    private String groupName;
    /**
     * 聊天室成员
     */
    private Set<String> members;


    public static final Group EMPTY_GROUP = new Group("empty", Collections.EMPTY_SET);



    /**
     * @param groupName
     * @param members
     */
    public Group(String groupName, Set<String> members) {
        this.groupName = groupName;
        this.members = members;
    }





}
