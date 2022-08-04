package com.edu.netty.chat.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.edu.netty.chat.protocal.ProtocolFrameDecoder;
import com.edu.netty.message.RpcRequestMessage;
import com.edu.netty.protocal.MessageCodec;
import com.edu.netty.protocal.SequenceIdGenerator;
import com.edu.netty.rpc.handler.RpcResponseMessageHandler;
import com.edu.netty.rpc.service.HelloService;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcManager {
    
    // 客户端唯一的 Channel 对象
    public static Channel channel = null;
    // 用于构建对象锁，保障 Channel 的单例唯一
    private static final Object obj = new Object();
    // 获取通道
    public static Channel getChannel(){
        if (channel != null) {
            return channel;
        }
        synchronized(obj) {
            if (channel != null) {
                return channel;
            }
            initChannel();
            return channel;
        }
    }

    /**
     * 初始化通道
     * @return
     */
    public static void initChannel() {
        final EventLoopGroup worker = new NioEventLoopGroup(3);
        LoggingHandler LOGGER_HANDLE = new LoggingHandler();
        MessageCodec MESSAGE_CODEC = new MessageCodec();
        RpcResponseMessageHandler RPC_RESPONSE_HANDLER = new RpcResponseMessageHandler();
        // 
        try {
            ChannelFuture channelFuture = new Bootstrap() 
                .group(worker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        channel.pipeline()
                            .addLast(new ProtocolFrameDecoder())
                            .addLast(LOGGER_HANDLE)
                            .addLast(MESSAGE_CODEC)
                            .addLast(RPC_RESPONSE_HANDLER);
                    }
                }).connect("localhost", 8080).sync();
            // 给通道赋值
            channel = channelFuture.channel();
            // 监控通道关闭
            channel.closeFuture().sync();
        } catch(InterruptedException e){
            e.printStackTrace();
        }finally {
            worker.shutdownGracefully();
        }
    }

    // 在主程序中完成项目接口的远程调用
    public static void main(String[] args) {

        HelloService HelloService = getProxyService(HelloService.class);
        String result = HelloService.sayHello("Alex");
        log.info("invoke remote Rpc Service get result :: {}", result);
    }

    /**
     * 我们希望屏蔽底层通信的细节，不关注参数处理，结果处理的网络交互的逻辑
     * 我们希望像调用本地方法一样去调用远程的方法。
     * ---------------------------------------------------------------------
     *  这样的需求正好是代理模式的适用范围，我们使用代理模式来解决这个问题。
     *
     *  代理模式的核心就是构建代理对象，在代理对象创建的时候织入需要增加的逻辑，
     *  然后使用代理对象来进行方法调用的时候
     *  我们就可以使用在创建代理对象的时候预定义的需要代理的行为.
     *
     * @param <T>
     * @param clazz
     * @return
     */
    public static <T> T getProxyService(Class<T> clazz) {

        // 使用 JDK 内置的动态代理模式，来创建代理对象
        // 需要三个参数
        // 1. 类加载器，这个我们可以借用需要代理的类的类加载器
        ClassLoader classLoader = clazz.getClassLoader();
        // 2. 需要代理的接口，<JDK 的动态代理基于接口实现>,
        // 这里需要的是一个接口的数组，
        Class<?>[] interfaces = new Class[]{clazz};
        // 3. 需要增强的代理对象，这里我们使用自己定义的 InvocationHandler 对象
        // 这个对象只有一个方法，这里面需要实现对于原本接口的方法的增强
        // 就是需要在这里发起网络调用，参数包装，数据解析，最终将结果返回出去
        InvocationHandler invocationHandler = new InvocationHandler() {
            /** 
             * invoke 函数共有单个参数，分别表示
             * 1.当前被代理的对象
             * 2.当前被代理的方法
             * 3.当前方法的参数
             * 最终返回方法调用后的结果，没有结果放回 null;
             */
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                /* 根据这些参数需要包装我们的请求，并最终解析网络请求的结果并处理成我们需要的结果对象来返回 */
                RpcRequestMessage request = new RpcRequestMessage(
                        SequenceIdGenerator.nextId(),
                        clazz.getClass().getName(),
                        method.getName(), 
                        method.getReturnType(),
                        method.getParameterTypes(),
                        args
                );
                getChannel().writeAndFlush(request);

                // 构建一个 Promise 容器，用于接收网路请求的结果
                Promise<Object> promise = new DefaultPromise<>(getChannel().eventLoop());

                // 将接收结果的容器缓存起来，方便我们根据请求的唯一 Id 来返回结果
                RpcResponseMessageHandler.PROMISE_MAPS.put(request.getSequenceId(), promise);

                // await() 方法是阻塞 等待结果，失败也不会抛出异常，需要自己判断
                promise.await();
                if(promise.isSuccess()) {
                    return promise.getNow();
                } else {
                   throw new RuntimeException(promise.cause());
                }
            }
        };
        Object o = Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
        // 将这个代理的对象放回出去
        return (T) o;
    }
}
