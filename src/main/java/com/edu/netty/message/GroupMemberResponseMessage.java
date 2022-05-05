package com.edu.netty.message;

import lombok.Data;
import lombok.ToString;

import java.util.Set;

@Data
@ToString(callSuper = true)
public class GroupMemberResponseMessage extends AbstractResponseMessage{

    private Set<String> members;
    public GroupMemberResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    public GroupMemberResponseMessage(Set<String> members) {
        this.members = members;
    }

    @Override
    public int getMessageType() {
        return Message.GROUP_MEMBER_RESPONSE_MESSAGE;
    }
}
