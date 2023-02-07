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
package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.rest.smart.HostStats;
import com.emc.rest.smart.ecs.Vdc;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static com.emc.object.ObjectConfig.PROPERTY_DISABLE_HEALTH_CHECK;
import static com.emc.object.ObjectConfig.PROPERTY_DISABLE_HOST_UPDATE;

public class RetryFilterTest extends AbstractS3ClientTest {
    @Override
    protected String getTestBucketPrefix() {
        return "s3-retry-test";
    }

    @Override
    protected S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config());
    }

    @Test
    public void testRetryFilter() throws Exception {
        int retryLimit = 3;
        final String flagMessage = "XXXXX";
        S3Config s3Config = ((S3JerseyClient) client).getS3Config();

        S3ObjectMetadata metadata = new S3ObjectMetadata().withContentLength(1).withContentType("text/plain");
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), "foo",
                new RetryInputStream(s3Config, flagMessage)).withObjectMetadata(metadata);
        try {
            client.putObject(request);
            Assert.fail("Retried more than retryLimit times");
        } catch (ProcessingException e) {
            Assert.assertEquals("Wrong exception thrown", flagMessage, e.getCause().getMessage());
        }

        s3Config.setRetryLimit(retryLimit + 1);

        request = new PutObjectRequest(getTestBucket(), "foo",
                new RetryInputStream(s3Config, flagMessage)).withObjectMetadata(metadata);
        client.putObject(request);
        byte[] content = client.readObject(getTestBucket(), "foo", byte[].class);
        Assert.assertEquals("Content wrong size", 1, content.length);
        Assert.assertEquals("Wrong content", (byte) 65, content[0]);

        try {
            request = new PutObjectRequest(getTestBucket(), "foo",
                    new RetryInputStream(null, null) {
                        @Override
                        public int read() throws IOException {
                            switch (callCount++) {
                                case 0:
                                    throw new S3Exception("should not retry", 400);
                                case 1:
                                    return 65;
                            }
                            return -1;
                        }
                    }).withObjectMetadata(metadata);
            client.putObject(request);
            Assert.fail("HTTP 400 was retried and should not be");
        } catch (ProcessingException e) {
            Assert.assertEquals("Wrong http code", 400, ((S3Exception) e.getCause()).getHttpCode());
        }

        try {
            request = new PutObjectRequest(getTestBucket(), "foo",
                    new RetryInputStream(null, null) {
                        @Override
                        public int read() throws IOException {
                            switch (callCount++) {
                                case 0:
                                    throw new S3Exception("should not retry", 501);
                                case 1:
                                    return 65;
                            }
                            return -1;
                        }
                    }).withObjectMetadata(metadata);
            client.putObject(request);
            Assert.fail("HTTP 501 was retried and should not be");
        } catch (ProcessingException e) {
            Assert.assertEquals("Wrong http code", 501, ((S3Exception) e.getCause()).getHttpCode());
        }

        try {
            request = new PutObjectRequest(getTestBucket(), "foo",
                    new RetryInputStream(null, null) {
                        @Override
                        public int read() throws IOException {
                            switch (callCount++) {
                                case 0:
                                    throw new RuntimeException(flagMessage);
                                case 1:
                                    return 65;
                            }
                            return -1;
                        }
                    }).withObjectMetadata(metadata);
            client.putObject(request);
            Assert.fail("RuntimeException was retried and should not be");
        } catch (ProcessingException e) {
            Assert.assertEquals("Wrong exception message", flagMessage, e.getCause().getMessage());
        }
    }

    @Test
    public void testDifferentNodes() throws Exception {
        int retryTimes = 4;
        final String flagMessage = "This will always fail";

        // tweak config to enable smart-client, have 2 hosts (same endpoint), and disable node discovery and pings
        S3Config s3Config = createS3Config();
        s3Config.setSmartClient(true);
        s3Config.setVdcs(Collections.singletonList(new Vdc(s3Config.getHost(), s3Config.getHost())));
        s3Config.setProperty(PROPERTY_DISABLE_HOST_UPDATE, true);
        s3Config.setProperty(PROPERTY_DISABLE_HEALTH_CHECK, true);
        s3Config.setRetryLimit(retryTimes);

        // need to re-create the client so these changes affect the load balancer, which is initialized at client creation
        client.destroy();
        client = new S3JerseyClient(s3Config);

        try {
            S3ObjectMetadata metadata = new S3ObjectMetadata().withContentLength(1).withContentType("text/plain");
            PutObjectRequest request = new PutObjectRequest(getTestBucket(), "foo",
                    new RetryInputStream(null, null) {
                        @Override
                        public int read() {
                            throw new S3Exception(flagMessage, 500);
                        }
                    }).withObjectMetadata(metadata);
            client.putObject(request);
            Assert.fail("500 error did not bubble an exception");
        } catch (ProcessingException e) {
            Assert.assertEquals("Wrong exception message", flagMessage, e.getCause().getMessage());
            Assert.assertEquals("Wrong http code", 500, ((S3Exception) e.getCause()).getHttpCode());
        }

        HostStats[] stats = ((S3JerseyClient) client).getLoadBalancer().getHostStats();
        // 2 hosts should be in the load balancer
        Assert.assertEquals(2, stats.length);
        // total requests should match (first request + retry count)
        long sumOfRequests = Arrays.stream(stats).mapToLong(HostStats::getTotalConnections).sum();
        Assert.assertEquals(sumOfRequests, retryTimes + 1);
        // each host should have at least 1 request
        Assert.assertTrue(stats[0].getTotalConnections() > 0);
        Assert.assertTrue(stats[1].getTotalConnections() > 0);
    }

    private class RetryInputStream extends InputStream {
        protected int callCount = 0;
        private long now;
        private long lastTime;
        private S3Config s3Config;
        private String flagMessage;

        public RetryInputStream(S3Config s3Config, String flagMessage) {
            this.s3Config = s3Config;
            this.flagMessage = flagMessage;
        }

        @Override
        public int read() throws IOException {
            int retryDelay = s3Config.getInitialRetryDelay() * (int) Math.pow(2, callCount - 1);
            switch (callCount++) {
                case 0:
                    lastTime = System.currentTimeMillis();
                    throw new S3Exception("foo", 500);
                case 1:
                    now = System.currentTimeMillis();
                    Assert.assertTrue("Retry delay for 1st error was not honored", now - lastTime >= retryDelay);
                    lastTime = now;
                    throw new S3Exception("bar", 503);
                case 2:
                    now = System.currentTimeMillis();
                    Assert.assertTrue("Retry delay for 2nd error was not honored", now - lastTime >= retryDelay);
                    lastTime = now;
                    throw new IOException("baz");
                case 3:
                    now = System.currentTimeMillis();
                    Assert.assertTrue("Retry delay for 3rd error was not honored", now - lastTime >= retryDelay);
                    lastTime = now;
                    throw new S3Exception(flagMessage, 500);
                case 4:
                    return 65;
            }
            return -1;
        }

        @Override
        public synchronized void reset() throws IOException {
        }

        @Override
        public boolean markSupported() {
            return true;
        }
    }
}
