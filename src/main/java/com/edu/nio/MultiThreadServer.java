package com.edu.nio;

import com.edu.util.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MultiThreadServer {
    public static void main(String[] args) throws IOException {

        Selector boss = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(8000));
        serverSocketChannel.register(boss, SelectionKey.OP_ACCEPT);

        // 为了避免 worker 多次创建，需要将 worker 创建出来，然后直接使用
        // Worker worker = new Worker("worker - 0");

        /*
        * 使用多个 Worker 实现多线程, 作为服务器应该创建多少 Worker 可以实现最佳的性能, 这里需要考虑服务器实际工作的任务决定，
        * > CPU 密集型，
        *       - 建议与 CPU 核心数相同
        * > 如果是 IO 密集型，
        *       - 建议与 参考 worker_num = (CPU_CORE_num + out_time/in_time)
        *       - CPU_CORE_num ： 参考 CPU 核心数
        *       - out_time ： 系统外部调用，非消耗CPU IO 操作的时间
        *       - in_time ：系统内运算，消耗CPU 操作的时间
        * > 动态获取当前系统的 CPU 核心数的方法
        *    - Runtime.getRuntime().availableProcessors()
        *    - 这个方法在容器化技术中存在 bug, 会获取到物理机器的 CPU 核心数，为非分配给容器的 CPU 核心数
        *      直到 jdk10+ 后才被修复
        */
        Worker[] workers = new Worker[2];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker("worker - " + i);
        }
        AtomicInteger index = new AtomicInteger(0);
        while (true) {
            boss.select();
            Iterator<SelectionKey> sKey = boss.selectedKeys().iterator();
            while (sKey.hasNext()) {
                SelectionKey key = sKey.next();
                sKey.remove();
                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel channel = server.accept();
                    channel.configureBlocking(false);
                    log.debug("accepted connection from {}", channel.getRemoteAddress());
                    // 将 boss 的 Selector 与 Worker 的 selector 关联起来.
                    // 这里需要将执行过程迁移到 Worker 线程执行，方便控制 worker-selector 中 register 与 select() 互相阻塞问题
                    // channel.register(worker.selector, SelectionKey.OP_READ);
                    // 单个 worker 工作状态
                    // worker.register(channel);

                    // 多个 worker 工作状态，按照轮询（round robin）的方式分配给 worker
                    workers[index.getAndIncrement() % workers.length].register(channel);
                    log.debug(" {}: register already.....", channel);
                }
            }
        }
    }

    /**
     * 工作线程
     */
    static class Worker implements Runnable {

        private Selector selector;

        private String name;
        private volatile Boolean isExecute = false;
        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        private Thread thread;

        public Worker (String name) {
            this.name = name;
        }

        public void register(SocketChannel channel) throws IOException {
            // 添加变量控制当前线程与 Selector 只会创建一份， 不会每次调用都创建一个新的线程
            if (!isExecute) {
                this.thread = new Thread(this, name);
                this.selector = Selector.open();
                thread.start();
                isExecute = true;
            }

            // 为了程序解耦，将这个添加注册事件作为 一个任务放入到队列中，从而传递给 work 进程执行，
            // 而不是调用这个方法的线程执行，从而避免了 register 方法 与 select 方法在不同的线程中
            // 执行而出现的阻塞问题

            queue.add (() -> {
                try {
                    channel.register(selector, SelectionKey.OP_READ);
                } catch (ClosedChannelException e) {
                    throw new RuntimeException(e);
                }
            });

            // 添加完任务后调用 selector 的唤醒方法，这样可以及时让 worker 线程处理queue 中的任务
            // 不会因为 select 方法阻塞而不往下执行
            selector.wakeup();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // 阻塞执行，直到监听到事件或者被唤醒
                    selector.select();
                    // 被唤醒后，或者监听到事件后，都先看一下队列中是否有任务，有任务先处理这个任务
                    Runnable poll = queue.poll();
                    if (poll != null) {
                        poll.run();
                    }
                    // 再看一下是否有事件，有事件就处理
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isReadable()) {
                            try {
                                SocketChannel channel = (SocketChannel) key.channel();
                                ByteBuffer buffer = ByteBuffer.allocate(128);
                                int read = channel.read(buffer);
                                if (read == -1) {
                                    log.debug("客户端断开连接.....");
                                    key.cancel();
                                }
                                buffer.flip();
                                ByteBufferUtil.debugAll(buffer);
                                log.debug("接收到消息 : {}", Charset.defaultCharset().decode(buffer));

                            }catch (IOException e) {
                                e.printStackTrace();
                                key.cancel();
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
