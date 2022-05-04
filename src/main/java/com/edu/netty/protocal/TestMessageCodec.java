package com.edu.netty.protocal;

import com.edu.netty.message.LoginRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class TestMessageCodec {

    // 这是一个 Sharable 的 Handler，提取出来，可以被多个 Channel 共享
    public static LoggingHandler LOGGER_HANDLE = new LoggingHandler(LogLevel.DEBUG);

    // 这个时不能共享的，
    // public static MessageCodec MESSAGE_CODER = new MessageCodec();

    // 这是可以共享的
    public static SharableMessageCodec SHARABLE_MESSAGE_CODEC = new SharableMessageCodec();

    public static void main(String[] args) throws Exception {

        EmbeddedChannel channel = new EmbeddedChannel(
                LOGGER_HANDLE,
                // 此处这个编码器可以配合自己的编解码器使用
                // 解决粘包与半包的问题
                new LengthFieldBasedFrameDecoder(
                        1024,
                        12,
                        4,
                        0,
                        0
                ),
                // MESSAGE_CODER,
                SHARABLE_MESSAGE_CODEC
        );
        LoginRequestMessage message = new LoginRequestMessage("Alex", "123456", "pika");

        // 写出数据，会使用 encode() 方法
        // channel.writeOutbound(message);

        // 写入数据，会使用 decode() 方法
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        new MessageCodec().encode(null, message, buf);

        // 对数据拆分
        ByteBuf s1 = buf.slice(0, 100);
        ByteBuf s2 = buf.slice(100, buf.readableBytes() - 100);

        // writeInbound() 和 writeOutbound() 方法在使用后，会执行 release()
        // 由于 buf 与 s1, s2 是同一块的内存，当第一次调用后，retain 计数被减少为 0，
        // 这时 s2 就不可用了，所以这里需要在 slice 方法调用后，手动增加 retain 计数
        s1.retain();

        // 将数据分为几次发送，模拟半包的情况
        channel.writeInbound(s1);
        channel.writeInbound(s2);
    }
}
