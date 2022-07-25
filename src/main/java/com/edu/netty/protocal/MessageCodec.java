package com.edu.netty.protocal;

import com.edu.netty.chat.config.Config;
import com.edu.netty.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;


/**
 * 自定义的消息的编解码器
 * ----------------------------------------------------------------------------
 *  > 测试发现，这个编解码器可以实现对自定义的消息的编解码。但是无法解决粘包，半包的问题，
 *  > 需要配合 LengthFieldBasedFrameCoder 来解决。
 */


/**
 * @Sharable 表示该类可以被多个 ChannelHandler 共享
 * ----------------------------------------------------------------------------
 *  > 如果一个编解码器不存储一些程序处理的中间状态，那么可以将该编解码器标记为 @Sharable，
 *  > 这样，它就可以被多个 ChannelHandler 共享。减少实例的数量，提高性能。
 *  ---------------------------------------------------------------------------
 *  > ByteToMessageCoder 上注释文档明确说明，这个抽象类的子类时不能被 @Sharable 标记的。
 *  > 这个父类在子类实例化的时候校验了 @Sharable 注解，如果有这个注解，就会抛出异常。
 *  实现共享的编解码器: 需要更换一个父类编码器
 * @see com.edu.netty.protocal.SharableMessageCodec
 */

// 继承了 ByteToMessageCodec 的类，因此不能共享
// @ChannelHandler.Sharable
@Slf4j
public class MessageCodec extends ByteToMessageCodec<Message> {

    /**
     * <b>编码器 </b> ： 消息出站前被编码器编码
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        // 1. => 4字节 -- 魔数：用于判断消息是否是可解析的主要依据
        out.writeBytes(new byte[]{'m','e', 's', 'g'});
        // 2. => 1字节 -- 版本, 字节的版本，方便做版本升级兼容处理
        out.writeByte(1);
        // 3. => 1字节 -- 系列化的方式：0 - jdk, 1 - json
        // 根据配置文件，获取系列化方式
        out.writeByte(Config.getSerializerAlgorithm().ordinal());
        // 4. => 1字节 -- 指令的类型: 见 Message 的类型 Type
        out.writeByte(msg.getMessageType());
        // 5. => 4字节 -- 消息的序号
        out.writeInt(msg.getSequenceId());
        // 6. => 1字节 -- 对齐填充位置 (一般都是 2 的倍数)
        out.writeByte(0);
        byte[] bytes = Config.getSerializerAlgorithm().serializer(msg);
        // 7. => 4字节 -- 消息的长度
        out.writeInt(bytes.length);
        // 8. => 消息的内容
        out.writeBytes(bytes);
    }

    /**
     * <b>解码器</b> : 消息入站后被解码器解码
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        int magicNum = in.readInt();
        byte version = in.readByte();
        // 获取序列化算法
        byte serializeType = in.readByte();
        byte messageType = in.readByte();
        int sequenceId = in.readInt();
        in.readByte();
        int length = in.readInt();

        log.debug("{}, {}, {}, {}, {}, {}", magicNum, version, serializeType, messageType, sequenceId, length);

        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);
        Serializer.Algorithm value = Serializer.Algorithm.values()[serializeType];
        // 反序列化的时候，需要指定对应的反序列化类型
        Class<?> messageClass = Message.getMessageClass(messageType);
        Object message = value.deserializer(messageClass, bytes);
        // 将数据放入到 out 集合中
        out.add(message);
        log.debug("{}", message.toString());
    }
}
