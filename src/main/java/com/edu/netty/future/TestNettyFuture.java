package com.edu.netty.future;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TestNettyFuture {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // Netty 中线程池对应以 EventLoop 存在

        NioEventLoopGroup group = new NioEventLoopGroup(2);

        Future<Integer> futureTask = group.next().submit(() -> {
            try {
                log.debug("execute code");
                TimeUnit.SECONDS.sleep(2);
                return 70;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        // log.debug("waiting result");
        // log.debug("result: {}", futureTask.get());

        futureTask.addListener((future) -> {
            // 任务完成后的回调, 所以这里肯定是有结果的，直接用非阻塞的方法拿到结果
            // 这里获取结果也不再是主线程中，而是再 NIOEventLoop 中执行
            log.debug("result: {}", future.getNow());

            // 用完之后关闭程序
            group.shutdownGracefully();
        });

    }
}
