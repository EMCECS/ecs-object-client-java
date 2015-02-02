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