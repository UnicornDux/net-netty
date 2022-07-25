package com.edu.netty.buf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.stream.Stream;

import static io.netty.buffer.ByteBufUtil.appendPrettyHexDump;
import static io.netty.util.internal.StringUtil.NEWLINE;

/**
 * ByteBuf 的创建实现
 * --------------------------------------------------
 *  > 内存分配模型 : 堆内存，直接内存
 *  > 池化 Buffer : 可以池化的 ByteBuf
 *     - 4.1+ 默认是开启的
 *     - 4.1 之前默认是关闭的
 *     - 安卓默认是关闭
 *     - -Dio.netty.allocator.type={unpooled|pooled} 可以指定
 */
@Slf4j
public class TestByteBuf {
    public static void main(String[] args) {

        // create_byteBuf();
        // slice_api();
        // duplicate_api();
        composite_api();
    }

    public static void create_byteBuf(){
        /**
         * 从下面的输出可以看到，ByteBuf的不同于 NIO 中使用的 ByteBuffer，
         * > ByteBuf 会随着写入数据不同而动态变化自身的容量,
         * > 容量变化时，扩容的规则是 :
         * 1. 当写入的数据总大小未超过 512 字节，则会进行扩容为 最靠近的 16 的 整数倍
         * 2. 当写入的数据总大小超过了 512 字节，则会 扩容为 最靠近的 2^n 大小，
         *
         */
        // heapBuffer 堆内存
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer();
        log.debug(byteBuf.getClass().getName());
        log(byteBuf);

        // directBuffer, buffer 直接内存
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();

        // 4.1+ 后默认启用了池化的 ByteBuf
        // 安卓平台默认还是非池化的 ByteBuf
        // -Dio.netty.allocator.type={unpooled|pooled} 配置
        log.debug(buffer.getClass().getName());
        log(buffer);

        Stream.generate(() -> "o")
                .limit(300)
                .forEach(str  -> buffer.writeBytes(str.getBytes(StandardCharsets.UTF_8)));
        log(buffer);

    }

    /**
     * 这里主要测试几个底层对拷贝做了优化的API 与一些注意事项
     * ------------------------------------------------------
     *  > release() 释放 ByteBuf 引用指针，配合内存回收
     *  > retain()  为 ByteBuf 增加引用指针，配合内存回收
     *
     *  以上方法主要是配合进行 ByteBuf 的内存回收, 这两个方法都是出自 ReferenceCounted 接口，
     *  每个ByteBuf 都实现了这个接口
     *  用于方便进行内存的回收，具体的回收事宜由对应的实现类自己来实现。
     */
    public static void slice_api(){

        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(10);
        byteBuf.writeBytes(new byte[]{'a', 'b', 'c', 'd','e','f','g', 'h','i','j'});
        ByteBuf slice1 = byteBuf.slice(0, 5);
        // 正确使用方式是自己需要维护一份引用，否则当原 buf 被回收后，slice 使用将会报错，
        // 自己维护一份引用，当这个slice 不再使用的时候，需要调用 release() 方法.
        slice1.retain();
        ByteBuf slice2 = byteBuf.slice(5, 5);
        slice2.retain();
        log(slice1);
        log(slice2);
        // slice 切片之后限定了长度不能被改变，不能写入新的数据,
        // slice1.writeBytes("java".getBytes(StandardCharsets.UTF_8));

        // getXxx(), setXxx() 都不会改变读取，写入指针的位置


        // 由于底层使用的是同一块内存空间，所有切片变化原 buf 也将变化,
        // 原 buf 的指针变化不会影响 slice, slice 维护了自己的指针
        slice1.setByte(1, 'z');
        log(slice1);
        log(byteBuf);

        slice1.release();
        slice2.release();
    }


    /**
     * 对 buf 进行了完整的切片，但是底层还是同一块内存
     */
    public static void duplicate_api(){

        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(10);
        byteBuf.writeBytes(new byte[]{'a', 'b', 'c', 'd','e','f','g', 'h','i','j'});

        ByteBuf duplicate = byteBuf.duplicate();
        duplicate.setByte(2,'k');
        log(duplicate);
        log(byteBuf);
    }

    /**
     * 组合 buf 的 api, 可以将多个 buf 组合成一个大的 buf 来统一访问, 实际底层也没有发生数据的拷贝
     */
    public static void composite_api(){
        // 一般情况下想要合并两个 buf, 需要将小的 buf 按照一定的规则拷贝到一个大的 buf
        ByteBuf buf1 = ByteBufAllocator.DEFAULT.buffer(5);
        buf1.writeBytes(new byte[]{1, 2, 3, 4, 5});
        ByteBuf buf2 = ByteBufAllocator.DEFAULT.buffer(6);
        buf2.writeBytes(new byte[]{6, 7, 8, 9, 10, 0});

        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(buf1.readableBytes() + buf2.readableBytes());
        buffer.writeBytes(buf1);
        buffer.writeBytes(buf2);
        log(buffer);

        CompositeByteBuf cbuf = ByteBufAllocator.DEFAULT.compositeBuffer();

        // 这个API 调用，需要注意第一个参数，必须是true 的时候才会启动写入指针，否则添加数据并没有变化
        cbuf.addComponents(true, buf1, buf2);
        log(cbuf);
    }


    /**
     * copy 开头的 API 底层使用的是深拷贝，对拷贝后的 buf 不会影响原本的 buf
     */

    public static void copy_api() {

    }


    /**
     * 打印缓冲区的内容工具
     * @param buffer
     */
    private static void log(ByteBuf buffer) {
        int length = buffer.readableBytes();
        int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
        StringBuilder buf = new StringBuilder(rows * 80 * 2);
        /*
            .append("read index:").append(buffer.readerIndex())
            .append(" write index:").append(buffer.writerIndex())
            .append(" capacity:").append(buffer.capacity())
            .append(NEWLINE);
        */
        StringJoiner joiner = new StringJoiner("\t");
        joiner.add("read index:").add("" + buffer.readerIndex())
              .add("write index:").add("" + buffer.writerIndex())
              .add("capacity:").add("" + buffer.capacity())
              .add(NEWLINE);
        buf.append(joiner);

        appendPrettyHexDump(buf, buffer);
        System.out.println(buf.toString());
    }
}
