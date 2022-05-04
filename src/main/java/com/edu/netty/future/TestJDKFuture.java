package com.edu.netty.future;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class TestJDKFuture {
    public static void main(String[] args) {

        // JDK 中的 Future 一般是与线程池结合使用的，
        // 1. 创建线程池
        ExecutorService service = Executors.newFixedThreadPool(2);
        // 2. 向线程池提交任务，并返回 Future 对象,
        //  - 如果提交的任务是 Callable 对象，可以抛出异常，返回结果
        //  - 如果提交的任务是 Runnable 对象，没有返回值，也不能抛异常
        Future<Integer> submit = service.submit(() -> {

            log.debug("task start.....");
            TimeUnit.SECONDS.sleep(1);
            return 50;
        });

        try {
            log.info("waiting result .....");
            log.debug("get result: {}",submit.get());
            service.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
