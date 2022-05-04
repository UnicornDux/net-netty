package com.edu.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

/**
 * 用于实现群聊的客户端对象
 */
public class GroupChatClient {

    // 需要连接到的服务器的地址
    private final String HOST = "127.0.0.1";
    // 服务器的端口
    private final int PORT = 8089;
    // 选择器
    private Selector selector;
    // 监听通道
    private SocketChannel socketChannel;
    // 客户端用户名称
    private String username;

    /* 在构造方法中完成初始化操作 */
    public GroupChatClient(){
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open(new InetSocketAddress(HOST,PORT));
            // 将socket配置为非阻塞
            socketChannel.configureBlocking(false);
            // 将channle 注册到selector
            socketChannel.register(selector, SelectionKey.OP_READ);
            // 获取到用户名
            username = socketChannel.getLocalAddress().toString().substring(1);
            System.out.println(username + " is OK...");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("初始化连接失败，请重试...");
        }
    }

    /* 向服务器发送消息 */
    private void sendInfo(String info){
        info = username + "说:" + info;
        try{
            socketChannel.write(ByteBuffer.wrap(info.getBytes()));

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("消息发送失败请重试......");
        }
    }

    /* 读取从服务器回复的消息 */
    private void readInfo(){
        try{
            // 获取当前selector中的通道总数
            int readChannles = selector.select();
            if (readChannles>0) {
                // 过滤出来符合条件的消息
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    // 取出所有注册到selector中的SelectionKey
                    SelectionKey key = iterator.next();
                    // 将所有的可读消息进行处理
                    if (key.isReadable()) {
                        SocketChannel sc = (SocketChannel) key.channel();
                        // 得到一个Buffer
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        // 读取消息到buffer
                        sc.read(buffer);
                        //将buffer中的消息转换为字符串打印出来
                        String msg = new String(buffer.array());
                        System.out.println(msg.trim());
                    }
                }
                iterator.remove();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* 开启客户端的主函数 */
    public static void main(String[] args) {

        GroupChatClient groupChatClient = new GroupChatClient();

        // 由于当前线程需要发送讯息到通道中，所以需要新开通道进行消息的监听
        new Thread(){
            @Override
            public void run() {
                while(true){
                    groupChatClient.readInfo();
                    try{
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();

        // 发送讯息到服务器端的客户端入口
        Scanner scanner = new Scanner(System.in);
        System.out.println("发送消息=>>");
        while(scanner.hasNextLine()){
            System.out.println("发送消息=>>");
            String msg = scanner.nextLine();
            groupChatClient.sendInfo(msg);
        }
    }
}
