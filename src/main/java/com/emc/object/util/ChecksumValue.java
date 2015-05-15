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
 * Represents a checksum value. Can yield the algorithm, offset (if applicable) and hex-value of the sum.
 */
public abstract class ChecksumValue {
    public abstract ChecksumAlgorithm getAlgorithm();

    public abstract long getOffset();

    public abstract String getValue();

    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Outputs this checksum in the format <code>{algorithm}/[{offset}/]{value}</code>,
     * where the offset may or may not be present.
     */
    public String toString(boolean includeByteCount) {
        String out = this.getAlgorithm().toString();
        if (includeByteCount) out += "/" + this.getOffset();
        out += "/" + getValue();
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChecksumValue)) return false;

        ChecksumValue that = (ChecksumValue) o;

        if (getOffset() != that.getOffset()) return false;
        if (getAlgorithm() != that.getAlgorithm()) return false;
        if (!getValue().equals(that.getValue())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getAlgorithm().hashCode();
        result = 31 * result + (int) (getOffset() ^ (getOffset() >>> 32));
        result = 31 * result + getValue().hashCode();
        return result;
    }
}
