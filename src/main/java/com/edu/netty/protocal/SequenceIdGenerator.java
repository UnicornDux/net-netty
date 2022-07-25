package com.edu.netty.protocal;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class SequenceIdGenerator {
    private static final AtomicInteger id = new AtomicInteger(0);

    public static int nextId() {
        return id.incrementAndGet();
    }
}

