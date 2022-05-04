package com.edu.netty.protocal;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class TestRespProtocol {

    /**
     * redis 服务器接收 redis 命令使用的是 RESP 协议, 协议如下：
     * -----------------------------------------------------------------
     *    - set name Alex
     * -----------------------------------------------------------------
     *  - 一上面的 set 命令为例
     * -------------------------
     *  *3
     *  $3
     *  set
     *  $4
     *  name
     *  $4
     *  Alex
     * -------------------------
     *  *3 表示命令的个数
     *  $num 是命令长度
     *  $str 是命令名称
     *  相互之间使用回车换行符分隔
     * -------------------------
     * @param args
     */
    final static byte[] line = "\r\n".getBytes();
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            ChannelFuture sync = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                ByteBuf byteBuf = ctx.alloc().buffer();

                                byteBuf.writeBytes("*3".getBytes());
                                byteBuf.writeBytes(line);

                                byteBuf.writeBytes("$3".getBytes());
                                byteBuf.writeBytes(line);

                                byteBuf.writeBytes("set".getBytes());
                                byteBuf.writeBytes(line);

                                byteBuf.writeBytes("$4".getBytes());
                                byteBuf.writeBytes(line);

                                byteBuf.writeBytes("name".getBytes());
                                byteBuf.writeBytes(line);

                                byteBuf.writeBytes("$4".getBytes());
                                byteBuf.writeBytes(line);

                                byteBuf.writeBytes("Alex".getBytes());
                                byteBuf.writeBytes(line);
                                ctx.writeAndFlush(byteBuf);
                            }
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                log.debug("收到服务器返回的数据：" + byteBuf.toString(StandardCharsets.UTF_8));
                            }
                        });
                    }
                }).connect("127.0.0.1", 6379).sync();
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }
}
