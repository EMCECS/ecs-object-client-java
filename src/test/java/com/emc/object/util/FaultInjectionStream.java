package com.emc.object.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FaultInjectionStream extends FilterInputStream {
    private int bytesBeforeFailure;
    private final IOException ioException;
    private final RuntimeException runtimeException;
    private int secondDelayBeforeThrowing;

    public FaultInjectionStream(InputStream in, int bytesBeforeFailure, IOException throwThis) {
        this(in, bytesBeforeFailure, throwThis, null);
    }

    public FaultInjectionStream(InputStream in, int bytesBeforeFailure, RuntimeException throwThis) {
        this(in, bytesBeforeFailure, null, throwThis);
    }

    private FaultInjectionStream(InputStream in, int bytesBeforeFailure, IOException ioException, RuntimeException runtimeException) {
        super(in);
        this.bytesBeforeFailure = bytesBeforeFailure;
        this.ioException = ioException;
        this.runtimeException = runtimeException;
    }

    @Override
    public int read() throws IOException {
        failIfReady();
        // we know there is at least 1 available byte
        decrementAvailableBytes(1);
        return super.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        failIfReady();
        int toRead = getAvailableBytes(len);
        int read = super.read(b, off, toRead);
        decrementAvailableBytes(read);
        return read;
    }

    private void failIfReady() throws IOException {
        if (bytesBeforeFailure <= 0) {
            if (secondDelayBeforeThrowing > 0) {
                try {
                    Thread.sleep(secondDelayBeforeThrowing * 1000L);
                } catch (InterruptedException ignored) {
                }
            }
            if (ioException != null) throw ioException;
            else throw runtimeException;
        }
    }

    public void setSecondDelayBeforeThrowing(int secondDelayBeforeThrowing) {
        this.secondDelayBeforeThrowing = secondDelayBeforeThrowing;
    }

    private int getAvailableBytes(int requested) {
        return Math.min(bytesBeforeFailure, requested);
    }

    private void decrementAvailableBytes(int bytesRead) {
        bytesBeforeFailure -= bytesRead;
    }
}
