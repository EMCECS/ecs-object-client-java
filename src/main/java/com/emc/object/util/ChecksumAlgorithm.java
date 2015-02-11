/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.util;

public enum ChecksumAlgorithm {
    SHA1("SHA-1"),
    MD5("MD5");

    private String digestName;

    private ChecksumAlgorithm(String digestName) {
        this.digestName = digestName;
    }

    public String getDigestName() {
        return this.digestName;
    }
}
