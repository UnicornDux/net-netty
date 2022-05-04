package com.edu.buffer;

import com.edu.util.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class TestBufferExam {

    /**
     * -----------------------------------------------------------------------------
     * 网络编程中经常会出现 粘包半包 的问题，主要是由于网络传输效率与socket 缓冲区的大小等原因造成
     * 数据在接收的时候被重新切割与组合的现象:
     * -----------------------------------------------------------------------------
     * > 例如: 你想给服务器发送如下三条消息 ::  .......  接受的到的数据形式可能是如下的形式:
     * --------------------------------            ---------------------------------
     *       Hello world \n                        Hello world \n I'm Alex\n Ho
     *       I'm Alex \n         =======>          w are you\n
     *       How are you \n
     * -----------------------------------------------------------------------------
     * 现在要求程序处理这个数据，让数据恢复以 \n 分割的形式
     * -------------------------------------------
     */

    public static void main(String[] args) {

        // 使用下面的一段模仿接收到的两条消息
        ByteBuffer source = ByteBuffer.allocate(40);
        source.put("Hello world \nI'm Alex\n Ho".getBytes(StandardCharsets.UTF_8));
        split(source);
        source.put("w are you\n".getBytes(StandardCharsets.UTF_8));
        split(source);
    }

    /**
     * 用于解析收到的数据
     *
     */
    public static void split(ByteBuffer source){
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            if (source.get(i) == '\n') {
                int length = i + 1 - source.position();
                ByteBuffer target = ByteBuffer.allocate(length);
                // 将内容读出后写入到目标的字节缓存区
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                ByteBufferUtil.debugAll(target);
            }
        }
        source.compact();
    }
}
