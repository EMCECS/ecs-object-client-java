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
package com.emc.object;

import com.emc.util.ConcurrentJunitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

@RunWith(ConcurrentJunitRunner.class)
public abstract class AbstractClientTest {
    private static final Logger log = LoggerFactory.getLogger(AbstractClientTest.class);
    private final ThreadLocal<String> testBucket = new ThreadLocal<>();

    /**
     * Implement to initialize the object client to be used for each test. Each subclass must keep a reference to the
     * client.
     */
    protected abstract void initClient() throws Exception;

    /**
     * Implement to create an arbitrary bucket or directory. Do not do anything if the
     * bucket/directory already exists.
     */
    protected abstract void createBucket(String bucketName) throws Exception;

    /**
     * Ditto for deleting a bucket/directory and all of its contents
     */
    protected abstract void cleanUpBucket(String bucketName) throws Exception;

    /**
     * Override to provide a different bucket prefix for each subclass.
     */
    protected String getTestBucketPrefix() {
        return "client-tests";
    }

    /**
     * Always call this method to get the bucket/directory to put object in for each test.
     *
     * @return a unique existing bucket for a particular test. will be cleaned up after the test automatically.
     */
    protected final String getTestBucket() {
        return testBucket.get();
    }

    @Before
    public final void initTestBucket() throws Exception {
        log.info("initializing client");
        initClient();

        String uuid = UUID.randomUUID().toString();

        // {prefix}-{$USER}-{uuid[-6]}
//        testBucket.set(getTestBucketPrefix() + "-" + System.getenv("USER") + "-" + uuid.substring(uuid.length() - 6));
//        log.info("creating test bucket " + getTestBucket());
//        createBucket(getTestBucket());
    }

    @After
    public final void destroyTestBucket() throws Exception {
//        log.info("cleaning up bucket " + getTestBucket());
//        cleanUpBucket(getTestBucket());
    }

    protected File createRandomTempFile(int size) throws Exception {
        File file = File.createTempFile("random-" + size, null);
        file.deleteOnExit();
        OutputStream out = new FileOutputStream(file);
        Random random = new Random();
        int bufferSize = 64 * 1024, written = 0, toWrite = bufferSize;
        byte[] buffer = new byte[bufferSize];
        while (written < size) {
            random.nextBytes(buffer);
            if (written + toWrite > size) toWrite = size - written;
            out.write(buffer, 0, toWrite);
            written += toWrite;
        }
        return file;
    }
}
