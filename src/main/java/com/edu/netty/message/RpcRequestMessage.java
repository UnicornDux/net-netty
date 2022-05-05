package com.edu.netty.message;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class RpcRequestMessage extends Message {


    /**
     * 调用接口的全限定名， 服务端根据它找到对应的实现
     */
    private String interfaceName;

    /**
     * 调用的方法名
     */
    private String methodName;

    /**
     * 返回结果
     */
    private Class<?> returnType;

    /**
     * 调用的方法参数类型数组
     */
    private Class<?>[] parameterTypes;

    /**
     * 调用的方法参数
     */
    private Object[] parameterValues;



    public RpcRequestMessage(
            int sequenceId,
            String interfaceName,
            String methodName,
            Class<?> returnType,
            Class<?>[] parameterTypes,
            Object[] parameterValues
    ) {
        super.setSequenceId(sequenceId);
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameterValues = parameterValues;
        this.returnType = returnType;
    }

    @Override
    public int getMessageType() {
        return Message.RPC_MESSAGE_TYPE_REQUEST;
    }
}
