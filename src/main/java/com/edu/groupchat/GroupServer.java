package com.edu.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * 多人用户的服务转发平台
 */
public class GroupServer {

    /* 引入选择器 */
    private Selector selector;
    /* 引入socket监听,监听的是一个tcp通道 */
    private ServerSocketChannel listenChannel;
    /* 监听端口 */
    private static final int PORT = 8089;

    /* 在方法的构造器中进行初始化 */
    public GroupServer(){
        try{
            //获取选择器
            selector = Selector.open();
            //获取监听
            listenChannel = ServerSocketChannel.open();
            //绑定监听的端口
            listenChannel.socket().bind(new InetSocketAddress(PORT));
            //设置为非阻塞模式
            listenChannel.configureBlocking(false);
            /* 将监听的频道注册到选择器中,注册的监听事件是（服务端监听接受新客户端请求） */
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* 监听过程 */
    private void  listen(){
        System.out.println("服务端监听线程监听客户端新建连接:===>>" + Thread.currentThread().getName());
        try{
            // 线程循环监听
            while (true){
                // 探听当前的事件数量 (可以获取当前selector中的事件个数)
                int count = selector.select();
                if(count>0){
                    // 遍历所有注册到Selector中的SelectionKey
                    Iterator<SelectionKey> iterable = selector.selectedKeys().iterator();
                    while(iterable.hasNext()){
                        SelectionKey key = iterable.next();
                        // 监听到accept的时候，
                        if(key.isAcceptable()){
                            // 接受连接通道
                            SocketChannel sc = listenChannel.accept();
                            // 将通道设置为阻塞态
                            sc.configureBlocking(false);
                            // 将sc注册到selector中，监听的是读取事件，就是要监听客户端发出的信息
                            sc.register(selector,SelectionKey.OP_READ);
                            // 提示用户上线
                            System.out.println(sc.getRemoteAddress() + "上线");
                        }
                        // 通道发送read事件，即通道是可读状态
                        if(key.isReadable()){
                            //对读取的消息进行处理
                            readMessage(key);
                        }
                        // 将当前的key移除，防止进行重复的读取
                        iterable.remove();
                    }
                }else{
                    System.out.println("等待......");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{

            // 将建立的连接都关闭
            try {
                selector.close();
                listenChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                selector=null;
                listenChannel=null;
            }
        }

    }

    /*
    * 读取客户端发送的信息
    *  */
    private void readMessage(SelectionKey key){
        // 获取关联的channle
        SocketChannel channle = null;
        try{
            channle = (SocketChannel) key.channel();
            // 创建接受消息的buffer
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int count = channle.read(buffer);
            // 根据读取的数据长度进行不同的处理
            if (count>0){
                String msg = new String(buffer.array());
                // 输出该消息
                System.out.println("来自客户端信息" + msg);

                // 向除了自己之外的其他客户端发送消息
                sendInfoToOtherClient(msg,channle);
            }

        } catch (IOException e) {
            e.printStackTrace();
            try{
                System.out.println(channle.getRemoteAddress() + "离线了");
                //取消注册
                key.cancel();;
                //关闭通道
                channle.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * 将消息转发到其他的客户端
     * @param msg
     * @param channle
     */
    private void sendInfoToOtherClient(String msg, SocketChannel channle) throws IOException {

        System.out.println("服务器转发消息中......");
        System.out.println("服务器转发消息给客户端线程:==>>" + Thread.currentThread().getName());
        //遍历所有的注册到Selector上的SocketChannle,并将自己self排除
        for(SelectionKey key: selector.keys()){
            //通过key取出对应的SocketChannle
            Channel targetchannle = key.channel();
            // 排除自己
            if(targetchannle instanceof SocketChannel && targetchannle!=channle){
                // 将目标channle SocketChannle
                SocketChannel dest = (SocketChannel) targetchannle;
                //将msg存储到buffer中，包装后发送出去
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                // 发送buffer
                dest.write(buffer);

            }
        }
    }

    /* 启动当前服务端的主函数 */
    public static void main(String[] args) {
        // 创建服务器对象
        GroupServer groupServer = new GroupServer();
        groupServer.listen();
    }

}
