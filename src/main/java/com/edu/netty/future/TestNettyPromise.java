package com.edu.netty.future;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TestNettyPromise {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 1. 获取 EventLoop 对象
        EventLoop eventLoop = new NioEventLoopGroup().next();

        // 2. 可以主动创建一个 Promise 对象，而不是程序执行过程中生成的
        DefaultPromise<Integer> promise = new DefaultPromise<>(eventLoop);

        // 3. 可以自己主动装载结果,
        //  - 任意线程执行任务，并将执行后的结果设置到 Promise 中，然后可以通过异步的方式来获取这个结果
        //  - 除了可以设置成功的结果，也可以设置异常的结果
        new Thread(() -> {
            try {
                log.debug("start code....");
                TimeUnit.SECONDS.sleep(1);
                promise.setSuccess(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                promise.setFailure(e.getCause());
            }
        }).start();

        // 3. 从其他线程中获取结果
        log.debug("waiting for result....");
        log.debug("result: {}", promise.get());
    }
}
