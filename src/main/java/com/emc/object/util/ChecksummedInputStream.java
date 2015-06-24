/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

public class ChecksummedInputStream extends InputStream {
    private InputStream delegate;
    private RunningChecksum checksum;
    private ChecksumValue verifyChecksum;

    public ChecksummedInputStream(InputStream delegate, ChecksumValue verifyChecksum)
            throws NoSuchAlgorithmException {
        this(delegate, new RunningChecksum(verifyChecksum.getAlgorithm()));
        this.verifyChecksum = verifyChecksum;
    }

    public ChecksummedInputStream(InputStream delegate, RunningChecksum checksum) {
        this.delegate = delegate;
        this.checksum = checksum;
    }

    @Override
    public int read() throws IOException {
        byte[] single = new byte[1];
        return read(single) == -1 ? -1 : (int) single[0] & 0xFF;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int value = delegate.read(b, off, len);
        if (value < 0) finish();
        else update(b, off, value);
        return value;
    }

    @Override
    public long skip(long n) throws IOException {
        long remaining = n;
        while (remaining > 0) {
            int toSkip = (int) Math.min(remaining, (long) Integer.MAX_VALUE);
            int skipped = skip(toSkip);
            if (skipped == 0) break;
            remaining -= skipped;
        }
        return n - remaining;
    }

    private int skip(int n) throws IOException {
        byte[] bytes = new byte[1024 * 64]; // 32K
        int toRead, read, total = 0;
        while (total < n) {
            toRead = Math.min(n - total, bytes.length);
            read = delegate.read(bytes, 0, toRead);
            if (read < 0) {
                finish();
                break;
            }
            update(bytes, 0, read);
            total += read;
        }
        return total;
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void mark(int readLimit) {
        throw new UnsupportedOperationException("mark not supported");
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException("mark not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    public ChecksumValue getChecksum() {
        return checksum;
    }

    private void update(byte[] bytes, int offset, int length) {
        checksum.update(bytes, offset, length);
    }

    private void finish() {
        if (verifyChecksum != null) {
            String referenceValue = verifyChecksum.getValue();
            String calculatedValue = checksum.getValue();
            if (!referenceValue.equals(calculatedValue))
                throw new ChecksumError("Checksum failure while reading stream", referenceValue, calculatedValue);
        }
    }
}
