/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.util;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamSegment extends SizedInputStream {
    private long offset;

    public InputStreamSegment(InputStream inputStream, long offset, long length) throws IOException {
        super(inputStream, length);
        this.offset = offset;

        long streamOffset = 0;
        while (streamOffset < offset) {
            streamOffset += inputStream.skip(offset - streamOffset);
        }
    }

    public long getOffset() {
        return offset;
    }
}
