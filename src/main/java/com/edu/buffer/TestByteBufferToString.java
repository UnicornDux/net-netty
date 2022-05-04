package com.edu.buffer;

import com.edu.util.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TestByteBufferToString {

    public static void main(String[] args) {

        // ---------------------------------------------------------------------
        // 字符串与bytebuffer 之间的相互转换
        // ---------------------------------------------------------------------
        // 1.字符串转换为 ByteBuffer转换之后还是写入模式)
        //--------------------------------
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put("hello".getBytes());
        ByteBufferUtil.debugAll(buffer);

        // 2. charset (转换之后,自动切换到读取模式)
        ByteBuffer hello = StandardCharsets.UTF_8.encode("hello");
        ByteBufferUtil.debugAll(hello);

        // 3. wrap (转换的工具类中提供的方式，转换为了读取模式)
        ByteBuffer wrap = ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8));
        ByteBufferUtil.debugAll(wrap);

        // ---------------------------------------------------------------------
        // ByteBuffer 转换为 String
        // ---------------------------
        String sWrap = StandardCharsets.UTF_8.decode(wrap).toString();
        System.out.println(sWrap);

        // 由于第一种方式写入之后没有切换为读取模式，需要做模式转换，否则无法读取到正
        // 确的数据。
        buffer.flip();
        String sBuf = StandardCharsets.UTF_8.decode(buffer).toString();
        System.out.println(sBuf);
    }
}
