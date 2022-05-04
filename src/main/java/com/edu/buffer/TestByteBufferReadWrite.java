package com.edu.buffer;

import com.edu.util.ByteBufferUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class TestByteBufferReadWrite {

    public static void main(String[] args) {
        // 测试基础的API
        TestBaseApi();

        // 测试读取与写入的模式切换API的区别
        TestChangModeApi();
        // 测试分散读出，同时定义多个bytebuffer组合起来
        // 一次读出多个ByteBuffer
        TestScatteringRead();
        // 测试 同时多个 ByteBuffer 集中写入
        TestGatheringWrites();
    }

    /**
     * -------------------------------------------------------
     * 测试一些基础的读取写入的API
     * -------------------------------------------------------
     */
    public static void TestBaseApi(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        byteBuffer.put(new byte[]{'a', 'b', 'c', 'd'});
        byteBuffer.flip();

        // 有几个比较特殊的读取方法 (rewind)
        byteBuffer.get(new byte[4]);
        ByteBufferUtil.debugAll(byteBuffer);
        // 重新读取, 将读取的指针恢复到开始位置
        byteBuffer.rewind();
        ByteBufferUtil.debugAll(byteBuffer);
        //System.out.println((char)byteBuffer.get());

        // make and reset
        System.out.println((char)byteBuffer.get());
        System.out.println((char)byteBuffer.get());
        // 在这个位置做上标记(make)
        byteBuffer.mark();
        System.out.println((char)byteBuffer.get());
        System.out.println((char)byteBuffer.get());
        // 回到之前做标记的位置(reset)
        byteBuffer.reset();
        System.out.println((char)byteBuffer.get());

        // get(index) 获取对应的位置的数据，不会改变position 指针的位置
        byteBuffer.rewind();
        ByteBufferUtil.debugAll(byteBuffer);
        System.out.println((char) byteBuffer.get(3));
        ByteBufferUtil.debugAll(byteBuffer);

    }

    /**
     * ---------------------------------------
     * 测试一些基础的写入与读取模式切换API的区别
     * ---------------------------------------
     */
    public static void TestChangModeApi(){

        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        byteBuffer.put((byte) 0x61);
        ByteBufferUtil.debugAll(byteBuffer);
        byteBuffer.put(new byte[]{0x62, 0x63, 0x65});
        ByteBufferUtil.debugAll(byteBuffer);

        // 此时直接读取再写入位置读取，不是正确的内容
        // System.out.println(byteBuffer.get());

        // 切换为读取模式, 读取位置移动到首位置
        byteBuffer.flip();
        System.out.println(byteBuffer.get());
        ByteBufferUtil.debugAll(byteBuffer);

        // 压缩缓存中原有的数据，并且切换到写入模式
        byteBuffer.compact();
        ByteBufferUtil.debugAll(byteBuffer);

        // 移动position位置到首位,切换为写入模式
        byteBuffer.clear();
        ByteBufferUtil.debugAll(byteBuffer);
    }

    /**
     * --------------------------------------------------
     * 测试分散写
     * --------------------------------------------------
     */
    public static void TestScatteringRead(){
        // 读取一个已知词分割位置(每个单词长度已知)的文件, 构建多个 ByteBuffer 缓存.
        // 从 channel 中将每个单词分别读取到一个Bytebuffer 缓存中
        try(FileChannel channel = new RandomAccessFile(
                "src/main/resources/file/word.file", "r").getChannel()
        )  {

            ByteBuffer b1 = ByteBuffer.allocate(3);
            ByteBuffer b2 = ByteBuffer.allocate(3);
            ByteBuffer b3 = ByteBuffer.allocate(5);
            channel.read(new ByteBuffer[]{b1, b2, b3});
            b1.flip();
            b2.flip();
            b3.flip();
            ByteBufferUtil.debugAll(b1);
            ByteBufferUtil.debugAll(b2);
            ByteBufferUtil.debugAll(b3);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * -------------------------------------------------
     * 测试集中写入，集中多个ByteBuffer一次写入
     * -------------------------------------------------
     */
    public static void  TestGatheringWrites(){
        ByteBuffer b1 = StandardCharsets.UTF_8.encode("hello");
        ByteBuffer b2 = StandardCharsets.UTF_8.encode("world");
        ByteBuffer b3 = StandardCharsets.UTF_8.encode("alex");

        try(FileChannel channel = new RandomAccessFile("src/main/resources/file/word.file","rw").getChannel()) {
            channel.write(new ByteBuffer[]{b1, b2, b3});
        }catch (Exception e) {
            System.out.println(e);

        }
    }
}
