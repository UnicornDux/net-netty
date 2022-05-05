package com.edu.netty.message;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GroupMemberRequestMessage extends Message{

    private String groupName;

    public GroupMemberRequestMessage(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public int getMessageType() {
        return Message.GROUP_MEMBER_REQUEST_MESSAGE;
    }
}
