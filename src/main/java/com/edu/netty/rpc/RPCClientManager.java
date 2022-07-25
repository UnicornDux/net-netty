package com.edu.netty.rpc;

import com.edu.netty.chat.protocal.ProtocolFrameDecoder;
import com.edu.netty.message.RpcRequestMessage;
import com.edu.netty.protocal.SequenceIdGenerator;
import com.edu.netty.protocal.SharableMessageCodec;
import com.edu.netty.rpc.handler.RpcResponseMessageHandler;
import com.edu.netty.rpc.service.HelloService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

@Slf4j
public class RPCClientManager {

    private static Channel channel = null;
    private static final Object LOCK = new Object();

    // 单例模式获取 channel 对象
    public static Channel getChannel() {
        if (channel != null) {
            return channel;
        }
        synchronized (LOCK) {
            if (channel != null) {
                return channel;
            }
            initChannel();
            return channel;
        }
    }
    private static void initChannel() {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGER_HANDLER = new LoggingHandler();
        SharableMessageCodec MESSAGE_CODEC = new SharableMessageCodec();
        RpcResponseMessageHandler RPC_RESPONSE_HANDLER = new RpcResponseMessageHandler();
        Bootstrap boot = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                    nioSocketChannel.pipeline()
                        .addLast(new ProtocolFrameDecoder())
                        .addLast(LOGGER_HANDLER)
                        .addLast(MESSAGE_CODEC)
                        .addLast(RPC_RESPONSE_HANDLER);
                }
            });
        try {
            channel = boot.connect("localhost", 8000).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 添加通道关闭事件，这里需要使用异步监听的方式，不能使用同步阻塞的方式
        channel.closeFuture().addListener(future -> {
            group.shutdownGracefully();
        });
    }

    public static void main(String[] args) {
        // 发送消息
        HelloService service = getProxyService(HelloService.class);
        String result = service.sayHello("Alex");
        String maria = service.sayHello("Maria");
        log.info("result: {}", result);
        log.info("maria: {}", maria);
    }

    public static <T> T getProxyService(Class<T> serviceClass){
        // 直接使用接口类型的类加载器
        ClassLoader classLoader = serviceClass.getClassLoader();
        // 创建代理需要实现的接口数组
        Class<?>[] interfaces = new Class[]{serviceClass};
        // 代理的行为
        Object o = Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {
            // 这里是我们需要做的事情
            // 1. 将方法调用转换为 消息对象
            RpcRequestMessage message = new RpcRequestMessage(
                    SequenceIdGenerator.nextId(),
                    serviceClass.getName(),
                    method.getName(),
                    method.getReturnType(),
                    method.getParameterTypes(),
                    args
            );
            // 2. 发送消息
            getChannel().writeAndFlush(message);
            // 3. 构建 Promise() 对象来接收返回值,
            // 构造器中传入一个线程 EventExecutor 指定 Promise 对象异步接收结果的线程
            Promise<Object> promise = new DefaultPromise<>(getChannel().eventLoop());
            RpcResponseMessageHandler.PROMISE_MAPS.put(message.getSequenceId(), promise);
            // 4. 等待 Promise 结果，await() 不管成功失败都不会抛出异常, 需要我们自己判断成功还是失败
            promise.await();
            if (promise.isSuccess()) {
                // 这个 getNow() 方法是非阻塞的
                return promise.getNow();
            } else {
                throw new RuntimeException(promise.cause());
            }
        });
        return (T) o;
    }
}
