package com.edu.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.SocketHandler;

/**
 * BIO 的模式是基于线程驱动的，每一个服务需要一个线程来提供服务，这些线程里面，大半部分时间是空闲的
 * 但是出现高并发的时候，线程过多，导致cpu高负荷甚至崩溃。
 */


public class BioServer {

    public static void main(String[] args) {

        ServerSocket serverSocket = null;

        InputStream in = null;
        OutputStream os = null;
        Socket socket = null;

        try{
            serverSocket = new ServerSocket(8888);
            // 1、当没有连接连过来的时候会一直阻塞在这里,
            while(true){
                socket = serverSocket.accept();
               /* in = socket.getInputStream();
                os = socket.getOutputStream();

                byte[] request = new byte[1024];

                // 2、当有连接进来的时候会阻塞在这里，一直去服务信息
                while(in.read(request) > 0){
                    System.out.println(request.toString());
                    os.write("OK".getBytes());
                }*/

                /* 在这里另开一个线程去处理对应的已连接的客户端，这样就可以实现服务端
                *  对多客户端的连接与处理，但是并发高的时候会出现cpu 高负荷甚至崩溃。
                *
                *  使用线程池对这个线程数量进行控制可以保护 cpu 资源,这样处理的客户端请求是有限的
                */

                //new Thread(new SocketHandler(socket)).start();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                in.close();
            } catch (IOException e) {
                in = null;
            }
            try {
                os.close();
            } catch (IOException e) {
                os = null;
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                serverSocket = null;
            }

        }
    }
}
