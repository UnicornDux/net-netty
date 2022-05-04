package com.edu.netty.protocal;

import com.edu.netty.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
 *  > 这里父类为 MessageToMessageCodec,
 */

/**
 * 必须和 LengthFieldBasedFrameCoder 配合使用, 保证当前解码器收到的消息时完整的消息，不会出现粘包，半包问题。
 */
@ChannelHandler.Sharable
@Slf4j
public class SharableMessageCodec extends MessageToMessageCodec<ByteBuf, Message> {

    /**
     * <b>编码器 </b> ： 消息出站前被编码器编码
     * @param ctx
     * @param msg
     * @param outList
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> outList) throws Exception {

        // 构建一个 ByteBuf 对象，将数据写入到 ByteBuf 对象中
        ByteBuf out = ctx.alloc().buffer();

        // 1. => 4字节 -- 魔数：用于判断消息是否是可解析的主要依据
        out.writeBytes(new byte[]{'m','e', 's', 'g'});
        // 2. => 1字节 -- 版本, 字节的版本，方便做版本升级兼容处理
        out.writeByte(1);
        // 3. => 1字节 -- 系列化的方式：0 - jdk, 1 - json
        out.writeByte(0);
        // 4. => 1字节 -- 指令的类型: 见 Message 的类型 Type
        out.writeByte(msg.getMessageType());
        // 5. => 4字节 -- 消息的序号
        out.writeInt(msg.getSequenceId());
        // 6. => 1字节 -- 对齐填充位置 (一般都是 2 的倍数)
        out.writeByte(0);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msg);
        byte[] bytes = bos.toByteArray();
        // 7. => 4字节 -- 消息的长度
        out.writeInt(bytes.length);
        // 8. => 消息的内容
        out.writeBytes(bytes);

        // 数据写入到 outList 中，往后续的 ChannelHandler 传递
        outList.add(out);
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
        byte serializeType = in.readByte();
        byte messageType = in.readByte();
        int sequenceId = in.readInt();
        in.readByte();
        int length = in.readInt();

        log.debug("{}, {}, {}, {}, {}, {}", magicNum, version, serializeType, messageType, sequenceId, length);

        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);
        if (serializeType == 0) {
            // jdk
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Message message = (Message) ois.readObject();
            // 将数据放入到 out 集合中
            out.add(message);
            log.debug("{}", message.toString());
        }else if (serializeType == 1) {
            // json
            String json = new String(bytes, Charset.defaultCharset());
            out.add(json);
        }else {
            log.debug("不支持的序列化方式");
        }
    }
}
