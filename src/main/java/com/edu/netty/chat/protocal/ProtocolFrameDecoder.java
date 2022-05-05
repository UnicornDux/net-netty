package com.edu.netty.chat.protocal;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;


/**
 *  由于这些处理数据的的参数数据与协议相关，一旦协议定下来之后，这些参数都不会随意改变，因此这里做了一层封装
 *  将这些参数封装这里。
 *  -----------
 *  继承自 LengthFieldBasedFrameDecoder, 避免程序中直接使用这个类，传递一堆与业务协议协议相关的参数
 */
public class ProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {

    public ProtocolFrameDecoder() {
        // 设置参数
        super(1024, 12, 4, 0, 0);
    }

    public ProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }
}
