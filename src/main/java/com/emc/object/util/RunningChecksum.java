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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Used to store, update and compute checksums
 */
public class RunningChecksum extends ChecksumValue {
    private ChecksumAlgorithm algorithm;
    private long offset;
    private MessageDigest digest;

    public RunningChecksum(ChecksumAlgorithm algorithm) throws NoSuchAlgorithmException {
        this.algorithm = algorithm;
        this.offset = 0;
        this.digest = MessageDigest.getInstance(algorithm.getDigestName());
    }

    /**
     * Updates the checksum with the given buffer's contents
     *
     * @param buffer data to update
     * @param offset start in buffer
     * @param length number of bytes to use from buffer starting at offset
     */
    public void update(byte[] buffer, int offset, int length) {
        this.digest.update(buffer, offset, length);
        this.offset += length;
    }

    @Override
    public ChecksumAlgorithm getAlgorithm() {
        return algorithm;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public String getValue() {

        // Clone the digest so we can pad current value for output
        MessageDigest tmpDigest;
        try {
            tmpDigest = (MessageDigest) digest.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone failed", e);
        }

        byte[] currDigest = tmpDigest.digest();

        // convert to hex string
        BigInteger bigInt = new BigInteger(1, currDigest);
        return String.format("%0" + (currDigest.length << 1) + "x", bigInt);
    }
}