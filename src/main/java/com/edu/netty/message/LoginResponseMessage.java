package com.edu.netty.message;

public class LoginResponseMessage extends Message {




    @Override
    public int getMessageType() {
        return Message.LOGIN_RESPONSE_MESSAGE;
    }
}
