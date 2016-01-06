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

import com.emc.object.ObjectConfig;
import com.emc.object.s3.jersey.GeoPinningFilter;
import com.emc.object.s3.jersey.GeoPinningRule;
import com.emc.object.s3.jersey.RetryFilter;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.rest.smart.Host;
import com.emc.rest.smart.HostStats;
import com.emc.rest.smart.HostVetoRule;
import com.emc.rest.smart.LoadBalancer;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.rest.smart.ecs.VdcHost;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.Filterable;
import com.sun.jersey.client.impl.ClientRequestImpl;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.MessageBodyWorkers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;

public class GeoPinningTest extends AbstractS3ClientTest {
    private S3Config s3Config;
    private List<Vdc> vdcs;

    @Override
    protected void initClient() throws Exception {
        s3Config = createS3Config();
        Assume.assumeFalse(s3Config.isUseVHost());

        // just going to use the same VDC thrice for lack of a geo env.
        List<? extends Host> hosts = s3Config.getVdcs().get(0).getHosts();
        Vdc vdc1 = new Vdc("vdc1", new ArrayList<Host>(hosts));
        Vdc vdc2 = new Vdc("vdc2", new ArrayList<Host>(hosts));
        Vdc vdc3 = new Vdc("vdc3", new ArrayList<Host>(hosts));

        vdcs = Arrays.asList(vdc1, vdc2, vdc3);

        String proxyUri = s3Config.getPropAsString(ObjectConfig.PROPERTY_PROXY_URI);
        s3Config = new S3Config(s3Config.getProtocol(), vdc1, vdc2, vdc3).withPort(s3Config.getPort())
                .withIdentity(s3Config.getIdentity()).withSecretKey(s3Config.getSecretKey());
        if (proxyUri != null) s3Config.setProperty(ObjectConfig.PROPERTY_PROXY_URI, proxyUri);

        s3Config.setGeoPinningEnabled(true);

        client = new S3JerseyClient(s3Config);

        Thread.sleep(500); // wait for polling daemon to finish initial poll
    }

    @Test
    public void testGuidExtraction() throws Exception {
        Assert.assertEquals("my/object/key", GeoPinningFilter.getGeoId(getTestBucket(), "my/object/key"));
        Assert.assertEquals("/my/object/key", GeoPinningFilter.getGeoId(getTestBucket(), "/my/object/key"));

        String bucketName = getTestBucket();
        Assert.assertEquals(bucketName, GeoPinningFilter.getGeoId(bucketName, null));
        Assert.assertEquals(bucketName, GeoPinningFilter.getGeoId(bucketName, ""));
    }

    @Test
    public void testGeoPinningAlgorithm() {
        String guid = "Hello GeoPinning";
        int hashNum = 0xa3fce8;

        Assert.assertEquals(0, GeoPinningFilter.getGeoPinIndex(guid, 1));
        Assert.assertEquals(hashNum % 2, GeoPinningFilter.getGeoPinIndex(guid, 2));
        Assert.assertEquals(hashNum % 3, GeoPinningFilter.getGeoPinIndex(guid, 3));
        Assert.assertEquals(hashNum % 4, GeoPinningFilter.getGeoPinIndex(guid, 4));
        Assert.assertEquals(hashNum % 5, GeoPinningFilter.getGeoPinIndex(guid, 5));
        Assert.assertEquals(hashNum % 6, GeoPinningFilter.getGeoPinIndex(guid, 6));
        Assert.assertEquals(hashNum % 7, GeoPinningFilter.getGeoPinIndex(guid, 7));
        Assert.assertEquals(hashNum % 8, GeoPinningFilter.getGeoPinIndex(guid, 8));
        Assert.assertEquals(hashNum % 9, GeoPinningFilter.getGeoPinIndex(guid, 9));
    }

    @Test
    public void testVetoRule() {
        Vdc good = new Vdc("good1", "good2", "good3");
        Vdc bad = new Vdc("bad1", "bad2", "bad3");
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(GeoPinningRule.PROP_GEO_PINNED_VDC, good);

        HostVetoRule geoPinningRule = new GeoPinningRule();

        Assert.assertFalse(geoPinningRule.shouldVeto(good.getHosts().get(0), properties));
        Assert.assertFalse(geoPinningRule.shouldVeto(good.getHosts().get(1), properties));
        Assert.assertFalse(geoPinningRule.shouldVeto(good.getHosts().get(2), properties));
        Assert.assertTrue(geoPinningRule.shouldVeto(bad.getHosts().get(0), properties));
        Assert.assertTrue(geoPinningRule.shouldVeto(bad.getHosts().get(1), properties));
        Assert.assertTrue(geoPinningRule.shouldVeto(bad.getHosts().get(2), properties));
    }

    @Test
    public void testVdcDistribution() {
        String key1 = "my/object/key", key2 = "/my/object/key";
        String key3 = "/xyz123ABC_-=..//object";
        int hash1 = 0xbb8619, hash2 = 0x64b13e, hash3 = 0x0b6033;

        testKeyDistribution(key1, hash1 % vdcs.size());
        testKeyDistribution(key2, hash2 % vdcs.size());
        testKeyDistribution(key3, hash3 % vdcs.size());

        String bucket1 = "my-test-bucket", bucket2 = "foo-bar3baz", bucket3 = "test-bucket-12345-xxzz-blah";

        int bHash1 = 0xc6c1ae, bHash2 = 0x2d4526, bHash3 = 0x9e3f48;
        testBucketDistribution(bucket1, bHash1 % vdcs.size());
        testBucketDistribution(bucket2, bHash2 % vdcs.size());
        testBucketDistribution(bucket3, bHash3 % vdcs.size());
    }

    @Test
    public void testReadRetryFailoverInFilter() throws Exception {
        S3Config s3ConfigF = new S3Config(s3Config);
        s3ConfigF.setGeoReadRetryFailover(true);
        GeoPinningFilter filter = new GeoPinningFilter(s3ConfigF);

        String bucket = "foo";
        String key = "my/object/key";
        int geoIndex = 0xbb8619 % vdcs.size();
        DummyClient client = new DummyClient();
        client.addFilter(filter);

        // test no retry
        ClientRequest request = new ClientRequestImpl(new URI("http://s3.company.com"), null);
        request.setMethod("GET");
        request.getProperties().put(S3Constants.PROPERTY_BUCKET_NAME, bucket);
        request.getProperties().put(S3Constants.PROPERTY_OBJECT_KEY, key);
        client.handle(request);

        Assert.assertEquals(vdcs.get(geoIndex), request.getProperties().get(GeoPinningRule.PROP_GEO_PINNED_VDC));

        // test 1st retry
        int retries = 1;
        request.getProperties().put(RetryFilter.PROP_RETRY_COUNT, retries);
        client.handle(request);

        int retryIndex = (geoIndex + retries) % vdcs.size();
        Assert.assertEquals(vdcs.get(retryIndex), request.getProperties().get(GeoPinningRule.PROP_GEO_PINNED_VDC));

        // test 2nd retry
        retries++;
        request.getProperties().put(RetryFilter.PROP_RETRY_COUNT, retries);
        client.handle(request);

        retryIndex = (geoIndex + retries) % vdcs.size();
        Assert.assertEquals(vdcs.get(retryIndex), request.getProperties().get(GeoPinningRule.PROP_GEO_PINNED_VDC));

        // test 3rd retry (we have 3 VDCs, so this should go back to the primary)
        retries++;
        request.getProperties().put(RetryFilter.PROP_RETRY_COUNT, retries);
        client.handle(request);

        retryIndex = (geoIndex + retries) % vdcs.size();
        Assert.assertEquals(geoIndex, retryIndex);
        Assert.assertEquals(vdcs.get(retryIndex), request.getProperties().get(GeoPinningRule.PROP_GEO_PINNED_VDC));
    }

    protected void testKeyDistribution(String key, int vdcIndex) {
        LoadBalancer loadBalancer = ((S3JerseyClient) client).getLoadBalancer();
        loadBalancer.resetStats();

        // write the same object 10 times
        for (int i = 0; i < 10; i++) {
            client.putObject(getTestBucket(), key, "Hello GeoPinning!", "test/plain");
        }

        // check no errors and total count
        Assert.assertEquals(0, loadBalancer.getTotalErrors());
        Assert.assertEquals(10, loadBalancer.getTotalConnections());

        for (HostStats stats : loadBalancer.getHostStats()) {
            if (vdcs.get(vdcIndex).equals(((VdcHost) stats).getVdc())) {
                // all hosts in the appropriate VDC should have been used at least once
                Assert.assertTrue(stats.getTotalConnections() > 0);
            } else {
                // hosts in other VDCs should *not* be used
                Assert.assertEquals(0, stats.getTotalConnections());
            }
        }
    }

    protected void testBucketDistribution(String bucket, int vdcIndex) {
        LoadBalancer loadBalancer = ((S3JerseyClient) client).getLoadBalancer();
        loadBalancer.resetStats();

        // make some requests to the bucket
        int requestCount = 8;
        client.createBucket(bucket);
        client.getBucketAcl(bucket);
        client.getBucketLocation(bucket);
        client.getBucketLocation(bucket);
        client.getBucketLocation(bucket);
        client.getBucketLocation(bucket);
        client.getBucketLocation(bucket);
        client.deleteBucket(bucket);

        // check no errors and total count
        Assert.assertEquals(0, loadBalancer.getTotalErrors());
        Assert.assertEquals(requestCount, loadBalancer.getTotalConnections());

        for (HostStats stats : loadBalancer.getHostStats()) {
            if (vdcs.get(vdcIndex).equals(((VdcHost) stats).getVdc())) {
                // all hosts in the appropriate VDC should have been used at least once
                Assert.assertTrue(stats.getTotalConnections() > 0);
            } else {
                // hosts in other VDCs should *not* be used
                Assert.assertEquals(0, stats.getTotalConnections());
            }
        }
    }

    private class DummyClient extends Filterable {
        public DummyClient() {
            super(new ClientHandler() {
                @Override
                public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
                    return new ClientResponse(200, new InBoundHeaders(), new ByteArrayInputStream(new byte[0]), new DummyWorkers());
                }
            });
        }

        public ClientResponse handle(ClientRequest request) {
            return getHeadHandler().handle(request);
        }
    }

    private class DummyWorkers implements MessageBodyWorkers {
        @Override
        public Map<MediaType, List<MessageBodyReader>> getReaders(MediaType mediaType) {
            return null;
        }

        @Override
        public Map<MediaType, List<MessageBodyWriter>> getWriters(MediaType mediaType) {
            return null;
        }

        @Override
        public String readersToString(Map<MediaType, List<MessageBodyReader>> readers) {
            return null;
        }

        @Override
        public String writersToString(Map<MediaType, List<MessageBodyWriter>> writers) {
            return null;
        }

        @Override
        public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return null;
        }

        @Override
        public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return null;
        }

        @Override
        public <T> List<MediaType> getMessageBodyWriterMediaTypes(Class<T> type, Type genericType, Annotation[] annotations) {
            return null;
        }

        @Override
        public <T> MediaType getMessageBodyWriterMediaType(Class<T> type, Type genericType, Annotation[] annotations, List<MediaType> acceptableMediaTypes) {
            return null;
        }
    }
}
