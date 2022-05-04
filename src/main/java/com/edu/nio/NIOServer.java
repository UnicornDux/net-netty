package com.edu.nio;

import com.edu.util.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;  

/**
 * NIO是基于事件驱动的: (接收，读，写，连接，断开)
 *
 * NIO的三大核心组件：通道（Channel）、缓冲（Buffer）、选择器（Selector）
 *
 * --通道（Channel）
 *
 * 是传统IO中的Stream(流)的升级版。Stream是单向的、读写分离（inputstream和outputstream），
 * Channel是双向的，既可以进行读操作，又可以进行写操作。
 *
 * --缓冲（Buffer）
 *
 * Buffer可以理解为一块内存区域，可以写入数据，并且在之后读取它。
 *
 * --选择器（Selector）
 *
 * 选择器（Selector）可以实现一个单独的线程来监控多个注册在它上面的信道（Channel），通过一定的选择机制，实现多路复用的效果。
 *
 * 基于NIO中的 Selector 实现IO多路复用，将连接channel注册到 Selector 中进行管理
 *  > Selector 是一个提供channel注册服务的线程，可以同时对接多个channel,并在线程池中为channel适配，并寻找合适的线程处理channel;
 *    在NIO模型中线程数量大大降低，线程切换效率大幅提高。
 *  > Selector 可以监控多个channel，并且可以通过key来获取channel的注册信息，事件信息(以下是事件的种类)
 *    ---------------------------------------------------------------------------
 *    | accept  | connect  |  read  |  write  |  close  |  register  |  select  |
 *    ---------------------------------------------------------------------------
 *
 *
 */
@Slf4j
public class NIOServer {
    public static void main(String[] args) throws IOException {
        // 负责轮询是否有新的连接
        Selector serverSelector = Selector.open();
        // 负责轮询处理连接中的数据
        Selector clientSelector = Selector.open();

        new Thread(()->{
            try {
                // 对应IO编程中服务端启动
                ServerSocketChannel listenerChannel = ServerSocketChannel.open();
                listenerChannel.socket().bind(new InetSocketAddress(8000));
                // 默认是 阻塞模式(true) -- 这个设置影响的是 accept 方法，不会阻塞
                // 设置为 false 后，accept 方法就会立即返回，如果没有连接，那么返回 null,
                listenerChannel.configureBlocking(false);
                // op_accept 表示服务端检测到客户端连接，服务可以连接这个连接
                // 这个 selectedKey 只对 OP_ACCEPT 操作有效
                listenerChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
                while(true){
                    // 监测是否有新的连接，这里的 10 指的是阻塞的时间为10 ms, 没有参数的 select() 方法是阻塞的
                    if (serverSelector.select(10) > 0) {
                        /*
                         * selectedKey 是channel 被注册到selector上之后返回的key，
                         * 将来可以通过这个 key 可以获取到是那个 channel, 发生了什么事件
                         */
                        Set<SelectionKey> set = serverSelector.selectedKeys();
                        Iterator<SelectionKey> iterator = set.iterator();
                        while (iterator.hasNext()) {
                            SelectionKey next = iterator.next();
                            if (next.isAcceptable()) {
                                try {
                                    //(1) 每来一个新连接，不是创建一个新线程，而是将这个线程注册到clientSelector
                                    SocketChannel socketChannel = ((ServerSocketChannel) next.channel()).accept();
                                    log.debug("有新的连接...... {}", socketChannel);
                                    // 默认是 阻塞模式(true) -- 这个设置影响的是read() 方法，不会阻塞
                                    // 设置为 false 后，read 方法会立即返回, 如果没有数据将返回 0
                                    socketChannel.configureBlocking(false);


                                    // 给对应的 SelectedKey 绑定一个 ByteBuffer, 作为 SelectedKey 附件，伴随SelectedKey 存在
                                    // 方便进行 ByteBuffer 的灵活调整，扩容
                                    ByteBuffer byteBuffer = ByteBuffer.allocate(16);

                                    // op_read 表示通道中已经有了可以读取的数据，可以执行读取操作了
                                    socketChannel.register(clientSelector, SelectionKey.OP_READ, byteBuffer);
                                    log.debug("客户端注册...... {}", socketChannel);

                                    // selectedKey 上发生的事件， 要么进行处理，要么丢弃，不能不处理，否则将会进入循环处理。
                                    // select 会认为这个事件没处理，继续下发
                                    // next.cancel(); // 事件被丢弃
                                } finally {
                                    // selectedKeys 中监听的是 SelectedKey 中的事件，这个事件是 selector 添加进来的，
                                    // 但是事件被加入进来，处理完成了之后不会自动删除，需要我们自己编码进行删除，否则这个会一直存在
                                    // 下一次 循环中 Iterator 依旧会取到这个 SelectionKey
                                    iterator.remove();
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(()->{
            String name = Thread.currentThread().getName();
            try {
                while(true){
                    // (2)批量轮询那些连接有数据可以读取，这里的1指的是阻塞的时间是1ms
                    if (clientSelector.select(1)>0){
                        Set<SelectionKey> set = clientSelector.selectedKeys();
                        Iterator<SelectionKey> iterator = set.iterator();
                        while(iterator.hasNext()){
                            SelectionKey next = iterator.next();
                            if(next.isReadable()) {
                                try {
                                    SocketChannel clientChannel = (SocketChannel) next.channel();
                                    // 当我们需要做缓冲扩容的时候，这时候使用原本的局部的缓冲区，是没用的
                                    // ByteBuffer byteBuffer = ByteBuffer.allocate(128);

                                    // 获取 channel 中绑定的 ByteBuffer，这是注册到 selector 中的时候添加的附件
                                    // 与 SocketChannel 声明周期一致, 可以通过 SelectedKey 获取，或者重新绑定
                                    ByteBuffer byteBuffer = (ByteBuffer) next.attachment();

                                    // 读取数据，以块为单位，批量读取
                                    int read = clientChannel.read(byteBuffer);
                                    if (read == -1) {
                                        // 如果客户端正常通过 socketChannel.close() 关闭连接，
                                        // 同样会触发一个 op_read 事件, 这种情况下，read() 返回 -1，
                                        // 同样我们也需要将这个 SelectionKey 丢弃掉，否则会一直存在
                                        log.debug("客户端关闭连接...... {}", clientChannel);
                                        next.cancel();
                                    }else {

                                        // byteBuffer.flip();
                                        // ByteBufferUtil.debugAll(byteBuffer);
                                        // String msg = Charset.defaultCharset().newDecoder().decode(split).toString();
                                        // log.debug("线程 {}: 接收到客户端 : {}, 消息 ： {}", name, clientChannel, msg);

                                        /**
                                         * 边界问题：当接收的消息需要进行分隔，或者缓冲区根本就放不下的时候，需要进行 Buffer 的扩容
                                         */
                                        ByteBuffer split = split(byteBuffer);

                                        // 需要扩容
                                        if (byteBuffer.position() == byteBuffer.limit()) {
                                           log.debug("消息需要扩容");
                                           ByteBuffer newBuffer = ByteBuffer.allocate(byteBuffer.capacity() * 2);
                                           // 在扩容之前必定是先经历的 compact() 操作. 进入了写入的模式，
                                           // 这里想要全部读出来，应该再次切换回到读模式
                                           byteBuffer.flip();
                                           newBuffer.put(byteBuffer);
                                           // 将新的缓冲区与 SelectionKey 绑定
                                           next.attach(newBuffer);
                                        }
                                        if (split != null) {
                                            // 返回的是读取到的最后一条消息，因为之前都是一直往里面写入，这时候想要读取，
                                            // 需要切换模式
                                            split.flip();
                                            String msg = Charset.defaultCharset().newDecoder().decode(split).toString();
                                            log.debug("线程 {}: 接收到客户端 : {}, 消息 ： {}", name, clientChannel, msg);
                                        }
                                    }
                                } catch (IOException e) {
                                    // 当客户端连接断开的时候，会抛出异常，从而导致服务端不能继续提供服务，
                                    // 我们需要捕获这个异常，将这个异常处理一下
                                    // 这里需要注意的是，当客户端断开的时候会触发一个 read 事件，这个事件会被添加到 selectedKeys 中，
                                    // 但是这个事件并没有被处理，如果我们不处理会导致服务端在循环处理这个事件，
                                    // 所以这里需要丢弃这个事件，避免事件不能被处理
                                    e.printStackTrace();
                                    next.cancel();
                                } finally {
                                    // selectedKeys 中监听的是 SelectedKey 中的事件，这个事件是 selector 添加进来的，
                                    // 但是事件被加入进来，处理完成了之后不会自动删除，需要我们自己编码进行删除，否则这个会一直存在
                                    // 下一次 循环中 Iterator 依旧会取到这个 SelectionKey
                                    iterator.remove();
                                    // next.interestOps(SelectionKey.OP_READ);
                                }
                            }
                        }
                    }
                }
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     *  消息的拆分
     *  -------------------------------------------
     *  > 这里的一个简单的拆分逻辑是以回车符号进行消息的拆分
     *  -------------------------------------------
     * @param source
     */
    public static ByteBuffer split(ByteBuffer source){
        source.flip();
        ByteBuffer target = null;
        for (int i = 0; i < source.limit(); i++) {
            if (source.get(i) == '\n') {
                int length = i + 1 - source.position();
                target = ByteBuffer.allocate(length);
                // 将内容读出后写入到目标的字节缓存区
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                ByteBufferUtil.debugAll(target);
            }
        }
        // 此处，当消息长度超过了缓存区的大小，虽然进行了compact(),
        // 但是此时这个方法是没有效果的，看这个方法的实现，是将未读数据拷贝到缓存区的开始位置，
        // 此时由于都是未读取的数据，所以指针等位置都不会发生变化
        source.compact();
        return target;
    }
}
