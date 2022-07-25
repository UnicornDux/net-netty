package com.edu.netty.rpc.handler;

import com.edu.netty.message.RpcRequestMessage;
import com.edu.netty.message.RpcResponseMessage;
import com.edu.netty.rpc.service.HelloService;
import com.edu.netty.rpc.service.ServiceFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
public class RpcRequestMessageHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage message) {
        RpcResponseMessage response = new RpcResponseMessage();
        try {
            HelloService service = (HelloService) ServiceFactory
                    .getService(Class.forName(message.getInterfaceName()));
            // 获取到方法
            Method method = service.getClass().getMethod(message.getMethodName(), message.getParameterTypes());
            // 完成方法调用
            Object result = method.invoke(service, message.getParameterValues());
            log.debug(result.toString());

            // 将处理的结果封装成对应的返回的消息体
            response.setReturnValue(result);
            // 设置请求序号，表明这是哪一个请求的结果
            response.setSequenceId(message.getSequenceId());
        }catch (Exception e) {
            e.printStackTrace();
            // 这里需要对异常处理，避免服务器的大量报错堆栈信息返回给客户端，这样不够友好
            // 传输这样大量的信息，也不利于网路传输
            String msg = e.getCause().getMessage();
            response.setExceptionValue(new Exception("远程调用失败：" + msg));
        }
        // 将结果输出
        ctx.writeAndFlush(response);
    }
}
