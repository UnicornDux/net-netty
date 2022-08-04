package com.edu.netty.rpc.handler;

import com.edu.netty.message.RpcResponseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
// 当前我们这个 handler 是就有共享的数据的，记录了状态数据，需要我们自己处理并发问题
// 这里使用了 ConcurrentHashMap，这个类是线程安全的，所以这里可以使用 @Sharable 注解
@ChannelHandler.Sharable
public class RpcResponseMessageHandler extends SimpleChannelInboundHandler<RpcResponseMessage> {

    // 用来主进程与 NioEvent 线程之间的通信, 请求发送，存储请求的 promise，用来返回响应
    public static final Map<Integer, Promise<Object>> PROMISE_MAPS = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponseMessage response) {
        
        // 打印收到的消息
        log.debug(response.toString());

        // 获取到请求的 promise, 并移除这个 promise，占用内存越来越多
        Promise<Object> promise = PROMISE_MAPS.remove(response.getSequenceId());
        if (promise != null) {
            // 将响应结果封装成 promise
            if (response.getExceptionValue() != null) {
                promise.setFailure(response.getExceptionValue());
            } else {
                promise.setSuccess(response.getReturnValue());
            }
        }
    }
}
