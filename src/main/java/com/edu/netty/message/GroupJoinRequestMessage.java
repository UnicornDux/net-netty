package com.edu.netty.message;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class GroupJoinRequestMessage extends Message {

    private String userName;
    private String groupName;


    public GroupJoinRequestMessage(String userName, String groupName) {
        this.userName = userName;
        this.groupName = groupName;
    }

    @Override
    public int getMessageType() {
        return Message.GROUP_JOIN_REQUEST_MESSAGE;
    }
}
