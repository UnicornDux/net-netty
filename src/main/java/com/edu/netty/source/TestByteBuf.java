package com.edu.netty.source;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Slf4j
public class TestByteBuf {

    public static void main(String[] args) {

        NioEventLoopGroup group = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(group)
            .channel(NioServerSocketChannel.class)
            .childOption(
                    ChannelOption.RCVBUF_ALLOCATOR, 
                    new AdaptiveRecvByteBufAllocator(16,16,16)
            ).childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                    nioSocketChannel.pipeline()
                        .addLast(new LoggingHandler())
                        .addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                /**
                                 * msg 这里是从网络中来的，网络接收时使用的 bytebuf 是使用的什么
                                 * 对于 IO 操作的 ByteBuf, netty 默认强制使用的是 DirectByteBuffer，
                                 * netty 认为这才是最好的，效率最高的，不允许用户修改
                                 * .childOption(
                                 *    ChannelOption.RCVBUF_ALLOCATOR,
                                 *    new AdaptiveRecvByteBufAllocator()
                                 * )
                                 * 这里 RCVBUF_ALLOCATOR 是指的是接收缓冲区的分配器，可以通过参数
                                 * 来控制用于网络缓冲区的大小，
                                 * 默认使用的是 AdaptiveRecvByteBufAllocator，这是一个可以根据
                                 * 接收数据的情况自动进行调整的缓冲区分配器，
                                 *  > 初始分配的值是 DEFAULT_INITIAL=1024,
                                 *  > 分配的最大值是 DEFAULT_MAXIMUN=65535，
                                 *  > 分配的最小值是 DEFAULT_MINIMUM=64，
                                 */ 
                                log.debug("msg: {}", msg);

                                /**
                                 * 这里表示的是程序中手动创建的 ByteBuf，
                                 * -------------------------------------------------------------------
                                 *  > io.netty.allocator.type = unpooled   ——- 用于控制是否使用池化内存
                                 *  > io.netty.noPreferDirect = true       ——- 用于控制是否默认使用直接内存
                                 */ 
                                ByteBuf buffer = ctx.alloc().buffer();
                                // 在此观测内存分配的细节
                                log.debug(buffer.toString());
                            }
                        });
                }
            });
        ChannelFuture future = bootstrap.bind(8000);
        try {
            future.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }
}
