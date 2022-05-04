package com.edu.netty.pipline;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Handler 与 Pipeline 是我们关注的重点, 后续的业务代码主要与这两个组件相关。
 * ----------------------------------------------------------------------
 */


@Slf4j
public class TestPipeline {
    public static void main(String[] args) {
        new ServerBootstrap()
            .group(new NioEventLoopGroup())
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                    // 通过 channel 拿到 pipeline
                    ChannelPipeline pipeline = nioSocketChannel.pipeline();
                    // 添加 handler head --> h1 --> h2 --> h3 --> h4 --> h5 --> h6 --> tail
                    // 底层是双向链表 (head 与 tail 是隐藏的自动添加的handler)
                    pipeline.addLast("handler1", new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            // 读取内容的时候，会按照 head --> tail 顺序触发 InboundHandler
                            log.info("handler1: {}", msg);
                            // 如果不写这句，下面的 handler2 不会被触发, 触发是同类型的 handler
                            // super.channelRead(ctx, msg);这个方法底层是调用的 ctx.fireChannelRead(msg);
                            // 也可以直接调用 ctx.fireChannelRead(msg);
                            // 触发后续处理器的同时, 可以将处理的结果传递给后续的 handler
                            super.channelRead(ctx, msg);
                        }
                   });
                    pipeline.addLast("handler2", new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            log.info("handler2: {}", msg);
                            super.channelRead(ctx, msg);
                        }
                   });
                    pipeline.addLast("handler3", new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            log.info("handler3: {}", msg);
                            // 这里后续已经没有 入站处理器，可以不调用这个方法
                            // super.channelRead(ctx, msg);

                            // 写出点内容, outBoundHandler 只有服务器向外输出的时候才会执行，
                            // 执行的顺序是从 tail --> head 的顺序调用
                            // 这个方法会从 tail 节点往前查找对应的 outBoundHandler，并逐渐往前传递。
                            // nioSocketChannel.writeAndFlush(
                            //         ctx.alloc().buffer().writeBytes(
                            //                 ("handler3: " + msg).getBytes(StandardCharsets.UTF_8)
                            //         )
                            // );

                            // ----------------------------------------------------------------------
                            // 出站处理器中一个不能忽略的地方就是 ctx.writeAndFlush(msg); 这个方法
                            // 这个方法与上面的方法的不同之处在于，
                            // 这个方法触发后会直接从当前位置往 head 节点方向找对应的 outBoundHandler
                            // 如果找不到则不会处理，
                            ctx.writeAndFlush(
                                    ctx.alloc().buffer().writeBytes(
                                            ("handler3").getBytes(StandardCharsets.UTF_8)
                                    )
                            );
                        }
                    });
                    pipeline.addLast("handler4", new ChannelOutboundHandlerAdapter() {
                        @Override
                        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                            log.info("handler4: {}", msg);
                            super.write(ctx, msg, promise);
                        }
                    });
                    pipeline.addLast("handler5", new ChannelOutboundHandlerAdapter() {
                        @Override
                        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                            log.info("handler5: {}", msg);
                            super.write(ctx, msg, promise);
                        }
                    });
                    pipeline.addLast("handler6", new ChannelOutboundHandlerAdapter() {
                        @Override
                        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                            log.info("handler6: {}", msg);
                            super.write(ctx, msg, promise);
                        }
                    });
                }
            }).bind(8000);
    }
}
