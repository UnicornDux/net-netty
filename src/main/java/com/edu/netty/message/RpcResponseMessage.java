package com.edu.netty.message;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class RpcResponseMessage extends Message {

    /**
     * 返回值
     */
    private Object returnValue;

    /**
     * ExceptionValue: 异常值
     */
    private Throwable exceptionValue;

    @Override
    public int getMessageType() {
        return Message.RPC_MESSAGE_TYPE_RESPONSE;
    }
}
