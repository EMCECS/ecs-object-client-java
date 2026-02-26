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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.emc.object.ObjectConfig;
import com.emc.object.s3.jersey.GeoPinningRule;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.util.GeoPinningUtil;
import com.emc.rest.smart.Host;
import com.emc.rest.smart.HostStats;
import com.emc.rest.smart.HostVetoRule;
import com.emc.rest.smart.LoadBalancer;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.rest.smart.ecs.VdcHost;

public class GeoPinningTest extends AbstractS3ClientTest {
    private List<Vdc> vdcs;

    @Override
    protected S3Config createS3Config() throws Exception {
        S3Config s3Config = super.createS3Config();

        // won't work with VHost or if smart-client is disabled
        Assumptions.assumeFalse(s3Config.isUseVHost());
        Assumptions.assumeTrue(s3Config.isSmartClient());

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
        return s3Config;
    }

    @Override
    protected S3Client createS3Client() throws Exception {
        S3Client client = new S3JerseyClient(createS3Config());

        Thread.sleep(500); // wait for polling daemon to finish initial poll

        return client;
    }

    @Test
    public void testGuidExtraction() {
        Assertions.assertEquals("my/object/key", GeoPinningUtil.getGeoId(getTestBucket(), "my/object/key"));
        Assertions.assertEquals("/my/object/key", GeoPinningUtil.getGeoId(getTestBucket(), "/my/object/key"));

        String bucketName = getTestBucket();
        Assertions.assertEquals(bucketName, GeoPinningUtil.getGeoId(bucketName, null));
        Assertions.assertEquals(bucketName, GeoPinningUtil.getGeoId(bucketName, ""));
    }

    @Test
    public void testGeoPinningAlgorithm() {
        String guid = "Hello GeoPinning";
        int hashNum = 0xa3fce8;

        Assertions.assertEquals(0, GeoPinningUtil.getGeoPinIndex(guid, 1));
        Assertions.assertEquals(hashNum % 2, GeoPinningUtil.getGeoPinIndex(guid, 2));
        Assertions.assertEquals(hashNum % 3, GeoPinningUtil.getGeoPinIndex(guid, 3));
        Assertions.assertEquals(hashNum % 4, GeoPinningUtil.getGeoPinIndex(guid, 4));
        Assertions.assertEquals(hashNum % 5, GeoPinningUtil.getGeoPinIndex(guid, 5));
        Assertions.assertEquals(hashNum % 6, GeoPinningUtil.getGeoPinIndex(guid, 6));
        Assertions.assertEquals(hashNum % 7, GeoPinningUtil.getGeoPinIndex(guid, 7));
        Assertions.assertEquals(hashNum % 8, GeoPinningUtil.getGeoPinIndex(guid, 8));
        Assertions.assertEquals(hashNum % 9, GeoPinningUtil.getGeoPinIndex(guid, 9));
    }

    @Test
    public void testVetoRule() {
        Vdc good = new Vdc("good1", "good2", "good3");
        Vdc bad = new Vdc("bad1", "bad2", "bad3");
        Map<String, Object> properties = new HashMap<>();
        properties.put(GeoPinningRule.PROP_GEO_PINNED_VDC, good);

        HostVetoRule geoPinningRule = new GeoPinningRule();

        Assertions.assertFalse(geoPinningRule.shouldVeto(good.getHosts().get(0), properties));
        Assertions.assertFalse(geoPinningRule.shouldVeto(good.getHosts().get(1), properties));
        Assertions.assertFalse(geoPinningRule.shouldVeto(good.getHosts().get(2), properties));
        Assertions.assertTrue(geoPinningRule.shouldVeto(bad.getHosts().get(0), properties));
        Assertions.assertTrue(geoPinningRule.shouldVeto(bad.getHosts().get(1), properties));
        Assertions.assertTrue(geoPinningRule.shouldVeto(bad.getHosts().get(2), properties));
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

    protected void testKeyDistribution(String key, int vdcIndex) {
        LoadBalancer loadBalancer = ((S3JerseyClient) client).getLoadBalancer();
        loadBalancer.resetStats();

        // write the same object 10 times
        for (int i = 0; i < 10; i++) {
            client.putObject(getTestBucket(), key, "Hello GeoPinning!", "test/plain");
        }

        // check no errors and total count
        Assertions.assertEquals(0, loadBalancer.getTotalErrors());
        Assertions.assertEquals(10, loadBalancer.getTotalConnections());

        for (HostStats stats : loadBalancer.getHostStats()) {
            if (vdcs.get(vdcIndex).equals(((VdcHost) stats).getVdc())) {
                // all hosts in the appropriate VDC should have been used at least once
                Assertions.assertTrue(stats.getTotalConnections() > 0);
            } else {
                // hosts in other VDCs should *not* be used
                Assertions.assertEquals(0, stats.getTotalConnections());
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
        Assertions.assertEquals(0, loadBalancer.getTotalErrors());
        Assertions.assertEquals(requestCount, loadBalancer.getTotalConnections());

        for (HostStats stats : loadBalancer.getHostStats()) {
            if (vdcs.get(vdcIndex).equals(((VdcHost) stats).getVdc())) {
                // all hosts in the appropriate VDC should have been used at least once
                Assertions.assertTrue(stats.getTotalConnections() > 0);
            } else {
                // hosts in other VDCs should *not* be used
                Assertions.assertEquals(0, stats.getTotalConnections());
            }
        }
    }

}
