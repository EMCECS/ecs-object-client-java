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
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.Random;

@RunWith(ConcurrentJunitRunner.class)
public abstract class AbstractClientTest {
    private static final Logger l4j = Logger.getLogger(AbstractClientTest.class);
    private static final Random random = new Random();
    private final ThreadLocal<String> testBucket = new ThreadLocal<String>();

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
        l4j.info("initializing client");
        initClient();

        testBucket.set(getTestBucketPrefix() + "-" + System.getenv("USER") + "-" + (random.nextInt(8999) + 1000));
        l4j.info("creating test bucket " + getTestBucket());
        createBucket(getTestBucket());
    }

    @After
    public final void destroyTestBucket() throws Exception {
        l4j.info("cleaning up bucket " + getTestBucket());
        cleanUpBucket(getTestBucket());
    }
}
