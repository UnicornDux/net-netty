package com.edu.netty.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestMessage extends Message {

    private String userName;
    private String password;
    private String nickname;

    @Override
    public int getMessageType() {
        return Message.LOGIN_REQUEST_MESSAGE;
    }
}
