package com.edu.netty.sticky_splicing;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 *  LengthFieldBasedFrameDecoder (基于长度字段的帧解码器)
 * ------------------------------------------------------------
 * LTV 协议分隔消息，具有特殊的头部，固定长度的头部，指定了消息的一些元信息，服务端根据这些元信息来判断怎么切分消息
 * -----------------------------------------------------------------------------------------------
 *  maxFrameLength:     // 最大帧长度，超过这个长度将会报错
 *  lengthFieldOffset:  // 帧头相对起始位置偏移量
 *  lengthFieldLength:  // 帧头长度
 *  lengthAdjustment:   // 帧头后还有几个字节是内容开始的位置
 *  initialBytesToStrip:// 解析的结果从头部位置 剥离 几个位置的内容(头部或者其他标志位内容)
 */
public class LTCBasedDecoder {
    public static void main(String[] args) {


        EmbeddedChannel embeddedChannel = new EmbeddedChannel(
                /**
                 *  maxFrameLength:     // 最大帧长度，超过这个长度将会报错
                 *  lengthFieldOffset:  // 帧头相对起始位置偏移量
                 *  lengthFieldLength:  // 帧头长度
                 *  lengthAdjustment:   // 帧头后还有几个字节是内容开始的位置
                 *  initialBytesToStrip:// 解析的结果从头部位置 剥离 几个位置的内容(头部或者其他标志位内容)
                 *  ------------------------------------------------------------
                 *   参数设置的时候与数据写入时候的预设的时候一致
                 */
                new LengthFieldBasedFrameDecoder(
                        1024,
                        0,
                        4,
                        1,
                        4
                ),
                new LoggingHandler(LogLevel.DEBUG)
        );

        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
        send_message(byteBuf, "hello world om");
        send_message(byteBuf, "hello");

        //向模拟服务器输送数据
        embeddedChannel.writeInbound(byteBuf);

        // 向客户端输送数据
        // embeddedChannel.writeOneInbound(byteBuf);
    }

    /**
     * 发送消息
     *
     * @param byteBuf
     * @param content
     */
    private static void send_message(ByteBuf byteBuf, String content) {
        // 四个字节的长度位，一个版本位置，然后是内容
        byte[] bytes = content.getBytes();
        int length = bytes.length;
        // 写入长度, 高位字节序写入，int 占用四个 byte
        byteBuf.writeInt(length);  //
        byteBuf.writeByte('1');    // 写入一个版本位置
        byteBuf.writeBytes(bytes); // 写入内容，
    }
}
