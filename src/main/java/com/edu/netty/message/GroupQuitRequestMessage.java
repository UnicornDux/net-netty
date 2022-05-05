package com.edu.netty.message;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class GroupQuitRequestMessage extends Message{

    private String userName;

    private String groupName;

    public GroupQuitRequestMessage(String userName, String groupName) {
        this.userName = userName;
        this.groupName = groupName;
    }

    @Override
    public int getMessageType() {
        return Message.GROUP_QUIT_REQUEST_MESSAGE;
    }
}
