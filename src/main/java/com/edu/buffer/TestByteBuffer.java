package com.edu.buffer;


import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
/**
 * --------------------------------------------------------
 *  用于测试 bytebuffer 的使用
 *  --------------------------
 *  > 类似数组的设计
 *  > 1、capacity  容量
 *  > 2、position  写入/读取的位置
 *  > 3、limit     写入模式等于容量限制, 读取时等于实际的数据末尾位置
 *  -------------------------------------------------------
 *  flip()    切换到读取模式, position = 0, limit = position + size
 *  clear()   切换到写入模式, 就是恢复了position 指针位置为起点,不管数据有没有读取都将被覆盖
 *  compact() 切换到写入模式, 同时将未读完的数据往前移动, 即将 position 位置之后的数据移动到起点，position 移动到数据末尾
 */

@Slf4j
public class TestByteBuffer {
    public static void main(String[] args) {

       //FileChannel
       // 1、输入输出流, 2、 RandomAccessFile
       try(FileChannel channel = new FileInputStream(
               "src/main/resources/file/text.file").getChannel()
       ){
           // 准备缓冲区, 静态方法，划分处理啊一份缓冲区域, 缓冲区域需要限制大小
           // 如果内容过长，我们需要循环来读取，来获得全部的内容
           ByteBuffer buffer = ByteBuffer.allocate(10);
           while(true){
               // 从缓冲区中读取数据, 再写入 buffer 中.
               int len = channel.read(buffer);
               // 检测读取的长度, 当输入流中没有数据输入的时候，读取长度为 -1
               if (len == -1) {
                   break;
               }
               // 打印 buffer 的内容, flip()切换到buffer的读取模式
               buffer.flip();
               // 检测buffer中是否有内容
               while(buffer.hasRemaining()){
                   // get() 方法，没有参数时候，默认取出一个字节
                   byte bt = buffer.get();
                   log.debug ("读取到数据: {}",(char)bt);
               }
               // 清除掉缓冲区的内容, 切换到写入模式
               buffer.clear();
           }
       }catch (IOException e){
           log.error(e.getMessage());
       }
    }
}
