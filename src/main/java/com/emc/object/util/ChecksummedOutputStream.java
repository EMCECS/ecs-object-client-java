/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.util;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

public class ChecksummedOutputStream extends OutputStream {
    private OutputStream delegate;
    private RunningChecksum checksum;

    public ChecksummedOutputStream(OutputStream delegate, ChecksumAlgorithm algorithm) throws NoSuchAlgorithmException {
        this(delegate, new RunningChecksum(algorithm));
    }

    public ChecksummedOutputStream(OutputStream delegate, RunningChecksum checksum) {
        this.delegate = delegate;
        this.checksum = checksum;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        update(b, off, len);
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    public ChecksumValue getChecksum() {
        return checksum;
    }

    private void update(byte[] bytes, int offset, int length) {
        checksum.update(bytes, offset, length);
    }

}
