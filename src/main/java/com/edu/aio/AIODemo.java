package com.edu.aio;

import com.edu.util.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


/**
 * 异步 IO
 * ---------------------------------------------------------
 * > Windows 下，使用 IOCP 实现了真正的异步 IO
 * > Linux 下，异步 IO 底层是通过 多路复用实现，在性能上并没有多大优势，只是换成了一套新的 API
 */

@Slf4j
public class AIODemo {
    public static void main(String[] args) {

        try (AsynchronousFileChannel channel = AsynchronousFileChannel
                .open(Paths.get("src/main/resources/file/word.file"), StandardOpenOption.READ)
        ){
            ByteBuffer buffer = ByteBuffer.allocate(16);
            /**
             *  参数1： ByteBuffer
             *  参数2： 读取的起始位置
             *  参数3： 附件
             *  参数4： 回调函数
             */
            log.debug("read begin .......");
            channel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                // 读取成功
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    log.debug("read success: {} bytes", result);
                    attachment.flip();
                    ByteBufferUtil.debugAll(attachment);
                }

                // 读取失败
                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    log.error("read failure: {}", exc.getMessage());
                }
            });
            log.debug("read end .......");

            // 等待读取完成, 由于读取的回调线程是守护线程，所以不会影响主线程的执行
            // 当主线程结束的时候，读取线程也会结束, 这里需要阻塞主线程等待读取完成
            System.in.read();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
