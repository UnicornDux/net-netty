package com.edu.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class NIOClient {

    public static void main(String[] args) throws IOException {
        SocketChannel clientSocketChannel =  SocketChannel.open();
        clientSocketChannel.connect(new InetSocketAddress("localhost", 8000));
        System.out.println("waiting.......");
    }
}
