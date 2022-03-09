package com.emc.object.s3.lfu;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a source of object data that can be streamed in parallel parts of arbitrary ranges within the object.
 */
public interface LargeFileMultipartSource {
    /**
     * Returns the total size of the object data
     */
    long getTotalSize();

    /**
     * Returns a stream that will provide all the object's data
     */
    InputStream getCompleteDataStream() throws IOException;

    /**
     * Returns an _independent_ stream that provides only the specified range within the object data.
     * Note: this stream must be readable in parallel with streams of other parts/ranges.
     */
    InputStream getPartDataStream(long offset, long length) throws IOException;
}
