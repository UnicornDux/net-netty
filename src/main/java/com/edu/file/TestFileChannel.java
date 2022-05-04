package com.edu.file;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

@Slf4j
public class TestFileChannel {

    public static void main(String[] args) {

        TestFileChannelBase();
        // 测试使用 TransferTo 拷贝文件
        TestFileChannelTransferTo();
    }

    public static void TestFileChannelBase(){
        /**
         * ----------------------------------------------------
         * 注意: FileChannel 只能工作在阻塞模式下:
         * ----------------------------------------------------
         * > 获取 FileChannel的方式.以下三种对象都有getChannel() 方法
         *   * 通过 FileInputStream 获取的Channel 只能读取
         *   * 通过 FileOutputStream 获取的channel 只能写入
         *   * RandomAccessFile 获取的 channel 根据获取文件时的模式决定
         * ---------------------------------------------------
         */

        // try() 的这种写法会自动添加一个finally{} 将打开的对象关闭
        try(FileChannel channel = new RandomAccessFile("src/main/resources/file/txt.file","rw").getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(10);
            // ---------------------------------------------
            // channel 中读取数据填充ByteBuffer, 返回读取到的字节数，
            // -1 表示没有可以读取的数据，即表示读取结束
            int readBytes = channel.read(buffer);
            log.debug(readBytes + "");

            // ---------------------------------------------
            // 写入数据的时候应该使用一个循环从缓冲区拿数据，直到没有数据
            // channel.write(); 不能保证一次就把数据全部写入。
            // ---------------------------------------------
            // buffer.put(...);
            // buffer.flip();
            // while(buffer.hasRemaining()) {
            //    channel.write(buffer);
            // }

            //------------------------------------------------------------
            // 获取当前指针的位置
            // 指针在文件尾时得到的值为 -1,此时向文件写入内容,是追加内容。如果设置的位置
            // 大于文件的末尾位置，文档末尾与新内容之间会有空洞
            channel.position();
            // 设置指针的位置。
            long newPos = 807;
            channel.position(newPos);

            // 获取文件的大小
            channel.size();

            // channel 使用之后必须要关闭
            // FileInputStream 等对象的 close()的时候会隐式调用
            // channel 的close() 方法

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 内容在两个 FileChannel 之间传递，实现文件的拷贝
     */
    public static void TestFileChannelTransferTo(){

        try (
            FileChannel from = new FileInputStream("src/main/resources/file/txt.file").getChannel();
            FileChannel to = new FileInputStream("src/main/resources/file/word.file").getChannel();
        ){
            // transferTo() 底层会利用零拷贝技术进行优化
            // from.transferTo(0, from.size(), to);
            // 这个API最大只能操作2G的文件,想要多个进行传输，需要使用循环
            long size = from.size();
            // left 表示还剩余多少字节
            for (long left = size; left > 0;) {
                // transferTo() 返回当前读取到的字节数
                left -= from.transferTo((size - left), left, to);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

