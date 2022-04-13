package com.emc.object.s3.lfu;

import com.emc.object.util.InputStreamSegment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LargeFileMultipartFileSource implements LargeFileMultipartSource {
    private final File file;

    public LargeFileMultipartFileSource(File file) {
        this.file = file;
        // sanity check
        if (!file.exists() || !file.canRead())
            throw new IllegalArgumentException("cannot read file: " + file.getPath());
    }

    @Override
    public long getTotalSize() {
        return file.length();
    }

    @Override
    public InputStream getCompleteDataStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public InputStream getPartDataStream(long offset, long length) throws IOException {
        return new InputStreamSegment(new FileInputStream(file), offset, length);
    }
}
