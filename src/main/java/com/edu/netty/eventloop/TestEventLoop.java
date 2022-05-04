package com.edu.netty.eventloop;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.NettyRuntime;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class TestEventLoop {

    public static void main(String[] args) {
        // 这个实现可以处理 io, 普通任务，定时任务
        // 1. 构造方法，到底使用了几个线程 (如果没有传值，默认是 NettyRuntime.availableProcessors() * 2)
        log.debug("系统可用核心数：{}", NettyRuntime.availableProcessors());
        EventLoopGroup group = new NioEventLoopGroup(2);
        // 默认的实现，可以处理普通任务，定时任务
        // EventLoopGroup group = new DefaultEventLoopGroup();

        // 2. 获取下一个事件循环对象
        // 上面构建了两个事件循环对象，我们在这里取值几次发现是轮询取出的
        log.debug("当前： {}", group.next());
        log.debug("当前： {}", group.next());
        log.debug("当前： {}", group.next());
        log.debug("当前： {}", group.next());

        /**
         * 3. 执行普通任务
         * ----------------------------------------------------------------
         * 因为继承了 ScheduledExecutorService, 可以使用线程池的相关方法提交任务
         * 这样设计的好处 ：
         *     - 可以将一些任务异步执行，
         *     - 事件分发后，转移任务执行权
         */
        group.next().submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.debug("execute general task");
        });

        /**
         * 4. 执行定时任务
         * ----------------------------------------------------------------
         * 参数1: runnable 任务
         * 参数2: 延迟时间 initialDelay
         * 参数3: 间隔时间 period
         * 参数4: 时间单位 unit
         * ---------------------
         *  > 维护连接健康检查（心跳监测）
         */
        group.next().scheduleAtFixedRate(() -> {
           log.debug("execute scheduled task");
        }, 0, 1, TimeUnit.SECONDS);
    }
}
