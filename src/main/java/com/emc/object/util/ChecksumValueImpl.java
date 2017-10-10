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

/**
 * Represents a static checksum value.
 */
public class ChecksumValueImpl extends ChecksumValue {
    private ChecksumAlgorithm algorithm;
    private long offset;
    private byte[] byteValue;
    private String hexValue;

    public ChecksumValueImpl(ChecksumAlgorithm algorithm, long offset, byte[] byteValue) {
        this(algorithm, offset, byteValue, null);
    }

    public ChecksumValueImpl(ChecksumAlgorithm algorithm, long offset, String hexValue) {
        this(algorithm, offset, null, hexValue);
    }

    public ChecksumValueImpl(ChecksumAlgorithm algorithm, long offset, byte[] byteValue, String hexValue) {
        this.algorithm = algorithm;
        this.offset = offset;
        this.byteValue = byteValue;
        if (hexValue != null) this.hexValue = hexValue.replaceAll("\"", "").trim();
    }

    /**
     * Constructs a new checksum value from a header string of the format <code>{algorithm}/[{offset}/]{value}</code>,
     * where the offset may or may not be present.
     */
    public ChecksumValueImpl(String headerValue) {
        String[] parts = headerValue.split("/");
        this.algorithm = ChecksumAlgorithm.valueOf(parts[0]);
        if (parts.length > 2) {
            this.offset = Long.parseLong(parts[1]);
            this.hexValue = parts[2];
        } else {
            this.hexValue = parts[1];
        }
    }

    public ChecksumAlgorithm getAlgorithm() {
        return algorithm;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public byte[] getByteValue() {
        return byteValue;
    }

    public String getHexValue() {
        return hexValue;
    }
}
