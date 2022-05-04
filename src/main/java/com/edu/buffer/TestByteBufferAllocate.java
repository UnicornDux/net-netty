package com.edu.buffer;

import java.nio.ByteBuffer;

public class TestByteBufferAllocate {

    public static void main(String[] args) {
        // 分配的内存是固定的，不能进行动态的调整
        ByteBuffer allocate = ByteBuffer.allocate(10);
        // 使用的是 java.nio.HeapByteBuffer
        // --java 堆内存, 会受到垃圾回收的影响
        System.out.println(allocate);

        ByteBuffer allocateDirect = ByteBuffer.allocateDirect(10);
        // 使用的是 java.nio.DirectByteBuffer
        // --直接内存, 不会受到垃圾回收的影响(分配内存的效率较低，使用不当会造成内存泄露)
        System.out.println(allocateDirect);
    }

}
