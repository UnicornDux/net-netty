package com.edu.netty.chat.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.edu.util.ByteBufferUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NioBlockServer {

    public static void main(String[] args) throws IOException{
        BioBlockServer();
        BioNoBlockServer();
        SelectorServer();
        WriteServer();
    }

     /**
      * 阻塞方式的服务器 
      */
    public static void BioBlockServer() {
        new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            try {
                // 1.创建ServerSocketChannel
                ServerSocketChannel ssChannel = ServerSocketChannel.open();
                // 绑定监听端口
                ssChannel.bind(new InetSocketAddress(8090));
                // 声明一个集合来存储对应客户端的连接
                List<SocketChannel> socketChannels = new ArrayList<>();
                while(true) {
                    SocketChannel sc = ssChannel.accept();
                    log.debug("accepted, {}", sc.getRemoteAddress());
                    // 将当前请求连接的 channel 放入集合中
                    // 方便后面遍历进行事件处理
                    socketChannels.add(sc);
                    // 2.读取数据，(如果是多线程程序，这里可以让一个专职负责数据的读取)
                    socketChannels.forEach(item -> {
                        try {
                            log.debug("read data from {}", item.getRemoteAddress());
                            // 使用 ByteBuffer 从 channel 中读取数据
                            item.read(buffer);
                            // 读取到数据之后，将ByteBuffer 切换为读取模式
                            buffer.flip();
                            // 读取 buffer 中的内容
                            ByteBufferUtil.debugRead(buffer);
                            buffer.clear();
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    });
                }
            }catch(IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    /**
     * 非阻塞方式的服务器
     * */
    public static void BioNoBlockServer() {
        new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            try {
                // 1.创建ServerSocketChannel
                ServerSocketChannel ssChannel = ServerSocketChannel.open();
                // 配置服务端的 channel 为非阻塞模式,这时候，accept() 方法将不会阻塞。
                // 每次调用时会返回一个新的 channel，如果没有新的连接，则返回 null。
                ssChannel.configureBlocking(false);
                // 绑定监听端口
                ssChannel.bind(new InetSocketAddress(8090));

                // 声明一个集合来存储对应客户端的连接
                List<SocketChannel> socketChannels = new ArrayList<>();
                while (true) {
                    SocketChannel sc = ssChannel.accept();
                    if (sc != null) {
                        log.debug("accepted: {}", sc.getRemoteAddress());
                        // 将客户端的连接设置为非阻塞模式
                        // 这时候，read() 方法将不会阻塞。
                        // 如果没有数据可读，则返回 -1。
                        sc.configureBlocking(false);
                        // 将当前请求连接的 channel 放入集合中
                        socketChannels.add(sc);
                    }
                    // 2. 读取数据
                    socketChannels.forEach(item -> {
                        try {
                            int i = item.read(buffer);
                            if (i > 0) {
                                log.debug("read data from {}", item.getRemoteAddress());
                                buffer.flip();
                                ByteBufferUtil.debugRead(buffer);
                                buffer.clear();
                            }
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    });
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    public static void split (ByteBuffer buffer) {
        // 切换为读取模式
        buffer.flip();
        for (int i = 0; i < buffer.limit(); i++) {
            // 找到一条完整的消息
            if (buffer.get(i) == '\n') {
                // 找到完整消息后，构建一个足够缓冲区读取这部分数据并输出
                int length = i + i - buffer.position();
                ByteBuffer target = ByteBuffer.allocate(length);
                for(int j = 0; j < length; j++) {
                    target.put(buffer.get());
                }
                ByteBufferUtil.debugAll(target);
            }
        }
        // 压缩读取或的空间, 如果此次没有读取到一个完整包, buffer 被写满了
        // 这个压缩会让 buffer 恢复到写入的状态, 此时 position == limit
        buffer.compact();
    }
    /**
     * Selector 方式的服务器
     */ 
    public static void SelectorServer() {
        new Thread(()-> {
            try {
                Selector selector = Selector.open();
                ServerSocketChannel ssChannel = ServerSocketChannel.open();
                // 必须要设置 channel  为非阻塞模式才能与 selector 关联使用
                ssChannel.configureBlocking(false);
                ssChannel.bind(new InetSocketAddress(8090));
                // selectorKey 是注册到的事件管理器
                SelectionKey ssckey = ssChannel.register(selector, SelectionKey.OP_ACCEPT, null);
                log.debug("获取到的 SelectionKey 为：{}", ssckey);

                while(true) {
                    // 阻塞方法，直到有事件发生, 可以避免 CPU 的空转
                    selector.select();
                    // 获取事件的集合迭代器
                    Iterator<SelectionKey> selectorIter = selector.selectedKeys().iterator();
                    // 循环遍历 selector 中的事件
                    while(selectorIter.hasNext()) {
                        SelectionKey key = selectorIter.next();
                        // 处理完后需要将事件从 selector 中移除
                        // 因为上一句已经拿到了事件引用，这里可以提前移除事件，避免后续忘记处理
                        // 如果忘记移除，下一次还会触发事件，这时候并没有事件处理，导致了空指针
                        selectorIter.remove();
                        if (key.isAcceptable()) {
                            ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
                            SocketChannel sc = ssc.accept();
                            // 设置获取到的连接到服务器的 channel 为非阻塞模式
                            sc.configureBlocking(false);

                            // 为 每个 Channel 关联一个 ByteBuffer 附件，用于存储读取的数据
                            ByteBuffer buffer = ByteBuffer.allocate(16);
                            // 设置 channel 关注的事件为读事件
                            sc.register(selector, SelectionKey.OP_READ, buffer);

                        }else if(key.isReadable()){
                            try {
                                // 获取到附件中的 buffer 用来读取数据
                                ByteBuffer buffer = (ByteBuffer)key.attachment();
                                // 无论客户端时正常断开，还是异常断开，都会触发读事件
                                // 触发读取事件的一定时客户端的连接，强制转换为 SocketChannel
                                // 可以通过 event.channel() 获取到客户端的 channel, 
                                SocketChannel sc = (SocketChannel) key.channel();
                                // 返回读取到的数据的长度, 一次没读完会触发多次的读事件
                                int i = sc.read(buffer);
                                // 服务端正常断开的情况下，触发读取事件，但是读取的数据为 -1
                                if (i == -1) {
                                    log.debug("客户端断开连接");
                                    // 因为 channel 已经断开，所以要将其从 selector 中移除
                                    key.cancel();
                                }else {
                                    // 正常读取数据
                                    // 需要处理的消息边界的问题, 解决粘包与半包的问题
                                    // split 函数可以解决粘包的问题，不能解决半包的问题
                                    split(buffer);
                                    // 当出现了半包的情况，split 并不能处理，而是需要我们进行 
                                    // buffer 扩容的处理
                                    // 由于没有数据被读取，buffer 被填满的情况下，判断扩容的条件
                                    if (buffer.position() == buffer.limit()) {
                                        // 申请一个两倍的空间，然后将原本的数据拷贝到新的空间中
                                        ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                                        // 将新得附件绑定到 buffer 上替代原有的附件
                                        buffer.flip();
                                        newBuffer.put(buffer);
                                        key.attach(newBuffer);
                                    }
                                }
                            }catch(IOException e) {
                                // 非法断开连接，触发读取事件，
                                // 触发读取异常，需要服务端主动取消事件监听
                                e.printStackTrace();
                                // 因为 channel 已经断开，所以要将其从 selector 中移除
                                key.cancel();
                            }
                        }else if (key.isWritable()) {

                        }
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            } 
        }).start();
    }

    /**
     * 构建一个服务器端程序向客户端发送大量的数据报文，使得客户端接收的时候，无法一次接收全部的报文
     * 客户端会分多次接收.
     *
     *  --- 
     *
     *  服务端在发送报文的时候，可能一次也不会全部发送出去，我们这时候不能就这样阻塞在那边，只做这一件事情
     *  需要让 channel 监听可写事件，让没有写完的数据在下一次可写事件触发的时候被写出到 channel 中
     *  分多次输出到 channel 中，这样就可以让客户端接收到全部的数据了也能让程序可以
     *  处理一些其他 channel 的事件，不会出现其他连接事件被完全阻塞的状况
     *
     * @param buffer
     */
    public static void WriteServer(){
        // 测试数据写出得服务端，在客户端建立连接后，向客户端写出大量得数据
        new Thread(() -> {
            try {
                Selector selector = Selector.open();
                ServerSocketChannel ssChannel = ServerSocketChannel.open();
                ssChannel.configureBlocking(false);
                ssChannel.bind(new InetSocketAddress(8089));
                SelectionKey ssckey = ssChannel.register(selector, SelectionKey.OP_ACCEPT);
                log.debug("获取到的 SelectionKey 为：{}", ssckey);
                while ( true ) {
                    // 阻塞监听事件
                    selector.select();
                    // 获取事件监听得列表
                    Iterator<SelectionKey> selectorKeys = selector.selectedKeys().iterator();
                    while(selectorKeys.hasNext()) {
                        SelectionKey key = selectorKeys.next();
                        selectorKeys.remove();
                        // 如果是接受事件，则表示有新的客户端连接
                        if (key.isAcceptable()) {
                            // 接收事件接收到 
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            SocketChannel channel = serverSocketChannel.accept();
                            channel.configureBlocking(false);
                            channel.register(selector, SelectionKey.OP_READ);
                            log.debug("获取到的 SelectionKey 为：{}", key);

                            // 构建一个较长的消息来发送到客户端
                            String msg = Stream.generate(() -> "0").limit(3000000).collect(Collectors.toList()).toString();
                            ByteBuffer buffer = Charset.defaultCharset().encode(msg);
                            int num = channel.write(buffer);
                            //  这里显示当次发送的数据量
                            log.debug("发送的数据量为：{}", num);

                            // 由于数据量比较大，单次没有发送完毕, 这是由操作系统决定的，
                            // 以前，我们可能需要在这里设置一个循环，来发送这些数据，
                            // 由于数据量比较大，单次没有发送完毕, 这是由操作系统决定的，
                            //
                            // ---------------------------------------------------------------
                            // while(buffer.hasRemaining()) {
                            //     channel.write(buffer);
                            // }
                            // ---------------------------------------------------------------
                            // 由于数据没有发送完成，下一次事件循环的时候会触发一个可写事件
                            // 我们正好可以利用这一点，来处理这个问题 (注意需要在原来的基础上追加一个事件)
                            key.interestOps(SelectionKey.OP_WRITE + key.interestOps());
                            // 将没有写完的数据作为附件添加到 key 中
                            key.attach(buffer);

                        }else if (key.isReadable()) {

                        }else if (key.isWritable()) {
                            // 在可写事件中继续监听可写事件，并将数据写出到 channel 中
                            SocketChannel socketChannel = (SocketChannel)key.channel();
                            // 获取附件
                            ByteBuffer buffer = (ByteBuffer)key.attachment();
                            // 写出数据, 这里如果依然没有全部写出，下次还会触发可写事件
                            socketChannel.write(buffer);
                            // 需要注意的是：如果数据全部写出，需要将事件监听移除，同时将附件移除，
                            if (!buffer.hasRemaining()){
                                // 移除监听事件
                                key.interestOps(key.interestOps() - SelectionKey.OP_WRITE);
                                // 移除附件
                                key.attach(null);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


}
