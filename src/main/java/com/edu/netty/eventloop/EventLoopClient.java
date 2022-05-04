package com.edu.netty.eventloop;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

/* 客户端示例 */
@Slf4j
public class EventLoopClient {
    public static void main(String[] args) throws InterruptedException {
        Channel channel =  new Bootstrap()
            .group(new NioEventLoopGroup())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    channel.pipeline().addLast(new StringEncoder());
                }
            })
            // connect() 是一个异步非阻塞的方法，这个方法会在连接请求发出后直接返回，
            // 这时候连接并没有真正建立，此时需要 Sync() 方法阻塞等待连接建立完成。
            .connect("127.0.0.1", 8000)
            .sync()
            .channel();
         log.debug("客户端: {}", channel);

       /*
        * 在这打断点，多次发送消息, 由于这时候的 EventLoopGroup 是多线程在运行，不同于以前的单线程运行,
        * idea 工具中断点的模式 默认是 All, 所以我们在这里需要右键调整断点的模式:
        *    - All: 暂停了所有的线程，(默认方式，可以修改)
        *    - Thread: 只会终止执行到当前代码片段的线程，其他线程不受影响
        * --------------------------------------------------------------------------
        * 这里我们只需要停止主线程，而发送数据的 EventLoop 线程是需要正常运行，才能保证数据成功发送
        *   >>> 调整为 Thread 模式
        * --------------------------------------------------------------------------
        * 这里可以观测到使用一个Channel 发送数据的时候，服务端总是同一个 EventLoop 线程在处理
        *  [结论]:
        * channel 一旦与 某一个 EventLoop 绑定，后续这个channel 的所有操作都会在这个 EventLoop 中进行
        * 启动多个客户端程序，进行验证。
        */
        log.info("客户端: 发送消息");
    }
}
