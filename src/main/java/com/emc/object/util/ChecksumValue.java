/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
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
