package com.emc.object.util;

import java.io.IOException;
import java.io.OutputStream;

public class CloseEventOutputStream extends OutputStream {
    private OutputStream delegate;
    private Runnable runOnCLose;

    public CloseEventOutputStream(OutputStream delegate, Runnable runOnCLose) {
        this.delegate = delegate;
        this.runOnCLose = runOnCLose;
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
        runOnCLose.run();
    }
}
