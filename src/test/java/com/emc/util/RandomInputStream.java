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
package com.emc.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class RandomInputStream extends InputStream {
    Random random = new Random();
    private long size;
    private boolean closed = false;

    public RandomInputStream(long size) {
        this.size = size;
    }

    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("stream closed");
        if (size <= 0) return -1;
        size--;
        return random.nextInt(256); // 0 <= value < 256
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (size <= 0) return -1;
        if (len > size) len = (int) size;
        for (int i = 0; i < len; )
            for (int rnd = random.nextInt(), n = Math.min(len - i, 4); n-- > 0; rnd >>= 8)
                b[off + i++] = (byte) rnd;
        size -= len;
        return len;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0) throw new IllegalArgumentException("argument must be positive");
        if (n > size) n = size;
        size -= n; // 0 <= n <= size
        return n;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min((long) Integer.MAX_VALUE, size);
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}
