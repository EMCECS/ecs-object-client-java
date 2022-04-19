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

import com.emc.codec.CodecChain;
import com.emc.codec.encryption.BasicKeyProvider;
import com.emc.codec.encryption.EncryptionConstants;
import com.emc.codec.encryption.EncryptionUtil;
import com.emc.object.EncryptionConfig;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.jersey.FaultInjectionFilter;
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.util.RandomInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class S3EncryptionClientBasicTest extends S3JerseyClientTest {
    private static final Logger log = LoggerFactory.getLogger(S3EncryptionClientBasicTest.class);

    protected int keySize = 128;
    protected S3JerseyClient rclient;
    protected S3EncryptionClient eclient;
    protected String encodeSpec;

    private BasicKeyProvider _keyProvider;
    private KeyPair _masterKey;
    private KeyPair _oldKey;

    @Override
    protected String getTestBucketPrefix() {
        return "s3-encryption-client-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        rclient = new S3JerseyClient(createS3Config());
        EncryptionConfig eConfig = createEncryptionConfig();
        eclient = new S3EncryptionClient(createS3Config(), eConfig);
        encodeSpec = eConfig.getEncryptionSpec();
        if (eConfig.isCompressionEnabled()) encodeSpec = eConfig.getCompressionSpec() + "," + encodeSpec;
        return eclient;
    }

    protected EncryptionConfig createEncryptionConfig() throws Exception {
        return new EncryptionConfig(getKeyProvider()).withKeySize(keySize);
    }

    protected synchronized BasicKeyProvider getKeyProvider() throws Exception {
        if (_keyProvider == null) {
            _keyProvider = new BasicKeyProvider(getMasterKey(), getOldKey());
        }
        return _keyProvider;
    }

    protected synchronized KeyPair getMasterKey() throws Exception {
        if (_masterKey == null) loadKeys();
        return _masterKey;
    }

    protected synchronized KeyPair getOldKey() throws Exception {
        if (_oldKey == null) loadKeys();
        return _oldKey;
    }

    protected void loadKeys() throws Exception {
        Properties keyprops = new Properties();
        InputStream keystream = getClass().getClassLoader().getResourceAsStream("keys.properties");
        Assume.assumeNotNull(keystream);
        keyprops.load(keystream);

        _masterKey = EncryptionUtil.rsaKeyPairFromBase64(keyprops.getProperty("masterkey.public"), keyprops.getProperty("masterkey.private"));
        log.debug("Master key sizes: public: {} private: {}",
                ((RSAPublicKey) _masterKey.getPublic()).getModulus().bitLength(),
                ((RSAPrivateKey) _masterKey.getPrivate()).getModulus().bitLength());
        _oldKey = EncryptionUtil.rsaKeyPairFromBase64(keyprops.getProperty("oldkey.public"), keyprops.getProperty("oldkey.private"));
        log.debug("Old key sizes: public: {} private: {}",
                ((RSAPublicKey) _oldKey.getPublic()).getModulus().bitLength(),
                ((RSAPrivateKey) _oldKey.getPrivate()).getModulus().bitLength());
    }

    @Test
    public void testEncodeMeta() throws Exception {
        String key = "hello.txt";
        String content = "Hello World!";

        client.putObject(getTestBucket(), key, content, null);
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);

        Assert.assertEquals("unencrypted size incorrect", "12",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SIZE));
        Assert.assertEquals("encrypted size incorrect", 16, objectMetadata.getContentLength().longValue());
        Assert.assertEquals("unencrypted sha1 incorrect", "2ef7bde608ce5404e97d5f042f95f89f1c232871",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1));
        Assert.assertEquals("master key ID incorrect", getKeyProvider().getMasterKeyFingerprint(),
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_KEY_ID));
        Assert.assertNotNull("IV null", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_IV));
        Assert.assertNotNull("Object key", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_OBJECT_KEY));
        Assert.assertNotNull("Missing metadata signature",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_META_SIG));
    }

    @Test
    public void testWithUserMeta() {
        String key = "metadata-test";
        String content = "Hello Metadata!!";
        String m1 = "meta1", v1 = "value1", m2 = "meta2", v2 = "value2";
        S3ObjectMetadata metadata = new S3ObjectMetadata().addUserMetadata(m1, v1).addUserMetadata(m2, v2);
        client.putObject(new PutObjectRequest(getTestBucket(), key, content).withObjectMetadata(metadata));

        metadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(2, metadata.getUserMetadata().size());
        Assert.assertNotNull(metadata.getUserMetadata(m1));
        Assert.assertNotNull(metadata.getUserMetadata(m2));
        Assert.assertEquals(v1, metadata.getUserMetadata(m1));
        Assert.assertEquals(v2, metadata.getUserMetadata(m2));
    }

    @Test
    public void testStream() throws Exception {
        String key = "test-file.txt";
        InputStream rawInput = getClass().getClassLoader().getResourceAsStream("uncompressed.txt");
        Assume.assumeNotNull(rawInput);

        client.putObject(new PutObjectRequest(getTestBucket(), key, rawInput)
                .withObjectMetadata(new S3ObjectMetadata().withContentLength(2516125L)));
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);

        Assert.assertEquals("unencrypted size incorrect", "2516125",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SIZE));
        Assert.assertEquals("encrypted size incorrect", 2516128L, objectMetadata.getContentLength().longValue());
        Assert.assertEquals("unencrypted sha1 incorrect", "027e997e6b1dfc97b93eb28dc9a6804096d85873",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1));
        Assert.assertEquals("master key ID incorrect", getKeyProvider().getMasterKeyFingerprint(),
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_KEY_ID));
        Assert.assertNotNull("IV null", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_IV));
        Assert.assertNotNull("Object key", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_OBJECT_KEY));
        Assert.assertNotNull("Missing metadata signature",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_META_SIG));
    }

    // Test a stream > 4MB.
    @Test
    public void testLargeStream() throws Exception {
        String key = "big-stream.obj";
        int size = 5 * 1024 * 1024 + 13;
        RandomInputStream rs = new RandomInputStream(size);

        client.putObject(new PutObjectRequest(getTestBucket(), key, rs)
                .withObjectMetadata(new S3ObjectMetadata().withContentLength((long) size)));
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);

        // Make sure the checksum matches
        String sha1hex = DigestUtils.sha1Hex(client.readObject(getTestBucket(), key, byte[].class));

        assertNotNull("Missing SHA1 meta", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("SHA1 incorrect", sha1hex,
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("Stream length incorrect", size,
                Integer.parseInt(objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SIZE)));
    }

    @Test
    public void testRekey() throws Exception {
        String key = "rekey-test.txt";
        String content = "Hello World!";

        client.putObject(getTestBucket(), key, content, null);

        // shouldn't need to rekey as the master key has not changed
        Assert.assertFalse(eclient.rekey(getTestBucket(), key));

        // change master key
        getKeyProvider().setMasterKey(getOldKey());

        // now actually rekey
        Assert.assertTrue(eclient.rekey(getTestBucket(), key));

        // Read back and test
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);

        assertEquals("Content differs", content, client.readObject(getTestBucket(), key, String.class));
        assertEquals("unencrypted size incorrect", "12",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SIZE));
        assertEquals("encrypted size incorrect", 16, objectMetadata.getContentLength().longValue());
        assertEquals("unencrypted sha1 incorrect", "2ef7bde608ce5404e97d5f042f95f89f1c232871",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("master key ID incorrect", EncryptionUtil.getRsaPublicKeyFingerprint((RSAPublicKey) getOldKey().getPublic()),
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_KEY_ID));
        Assert.assertNotNull("IV null", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_IV));
        Assert.assertNotNull("Object key", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_OBJECT_KEY));
        Assert.assertNotNull("Missing metadata signature",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_META_SIG));
    }

    @Test
    public void testRekeyAcl() throws Exception {
        String key = "rekey-with-acl-test.txt";
        String content = "hello rekey with acl!";

        // custom ACL
        String identity = createS3Config().getIdentity();
        AccessControlList acl = new AccessControlList();
        acl.addGrants(new Grant(new CanonicalUser(identity, identity), Permission.FULL_CONTROL));
        acl.addGrants(new Grant(Group.ALL_USERS, Permission.FULL_CONTROL));

        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, content).withAcl(acl);
        client.putObject(request);

        // verify custom ACL
        Assert.assertTrue(client.getObjectAcl(getTestBucket(), key).getGrants()
                .contains(new Grant(Group.ALL_USERS, Permission.FULL_CONTROL)));

        // change master key
        getKeyProvider().setMasterKey(getOldKey());

        // now actually rekey
        Assert.assertTrue(eclient.rekey(getTestBucket(), key));

        // Read back and test
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);

        // verify rekey
        assertEquals("master key ID incorrect", EncryptionUtil.getRsaPublicKeyFingerprint((RSAPublicKey) getOldKey().getPublic()),
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_KEY_ID));

        // verify ACL
        acl = client.getObjectAcl(getTestBucket(), key);
        Assert.assertTrue(acl.getGrants().contains(new Grant(Group.ALL_USERS, Permission.FULL_CONTROL)));
    }

    @Test
    public void testErrorInStream() throws Exception {
        byte[] data = "Error in the stream!!".getBytes();
        String key = "stream-error-test";

        S3Config _config = createS3Config();
        _config.setFaultInjectionRate(1f);
        S3Client _client = new S3EncryptionClient(_config, createEncryptionConfig());

        try {
            _client.putObject(getTestBucket(), key, data, null);
        } catch (S3Exception e) {
            Assert.assertEquals(FaultInjectionFilter.FAULT_INJECTION_ERROR_CODE, e.getErrorCode());
        }
    }

    @Test
    public void testRetries() throws Exception {
        byte[] data = "Testing retries!!".getBytes();
        String key = "retry-test";

        S3Config _config = createS3Config();
        _config.setFaultInjectionRate(0.4f);
        _config.setRetryLimit(10);
        S3Client _client = new S3EncryptionClient(_config, createEncryptionConfig());

        // make sure we hit at least one error
        for (int i = 0; i < 6; i++) {
            _client.putObject(getTestBucket(), key, data, null);
            S3ObjectMetadata metadata = rclient.getObjectMetadata(getTestBucket(), key);
            Assert.assertEquals(encodeSpec, metadata.getUserMetadata(CodecChain.META_TRANSFORM_MODE));
        }
    }

    @Test
    public void testReuseRequestOnError() throws Exception {
        byte[] data = "You can reuse the request object on errors".getBytes();
        String key = "request-reuse-test";
        InputStream stream = new ErrorStream(new ByteArrayInputStream(data));

        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, stream);

        // first PUT
        eclient.getS3Config().setRetryLimit(0);
        try {
            eclient.putObject(request);
            Assert.fail("no error generated");
        } catch (Exception e) {
            while (e.getCause() != null && e.getCause() != e) {
                e = (Exception) e.getCause();
            }
            if (!(e instanceof S3Exception)) throw e;
            if (!e.getMessage().equals("foo")) throw e;
            // second PUT
            eclient.putObject(request);
        }

        // check mode UMD
        S3ObjectMetadata metadata = rclient.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(encodeSpec, metadata.getUserMetadata(CodecChain.META_TRANSFORM_MODE));
    }

    // the following methods aren't supported in the encryption client

    @Ignore
    @Override
    public void testReadObjectStreamRange() {
    }

    @Ignore
    @Override
    public void testUpdateObjectWithRange() {
    }

    @Ignore
    @Override
    public void testSingleMultipartUploadMostSimpleOnePart() {
    }

    @Ignore
    @Override
    public void testSingleMultipartUploadMostSimple() {
    }

    @Ignore
    @Override
    public void testSingleMultipartUploadSimple() {
    }

    @Ignore
    @Override
    public void testMultiThreadMultipartUploadMostSimple() {
    }

    @Ignore
    @Override
    public void testLargeObjectContentLength() {
    }

    @Ignore
    @Override
    public void testSingleMultipartUploadListParts() {
    }

    @Ignore
    @Override
    public void testMultiThreadMultipartUploadListPartsPagination() {
    }

    @Ignore
    @Override
    public void testAppendObject() {
    }

    // the following methods are unnecessary and/or do not test anything related to encryption

    @Ignore
    @Override
    public void testCreateExistingBucket() {
    }

    @Ignore
    @Override
    public void testListBuckets() {
    }

    @Ignore
    @Override
    public void testListBucketsReq() {
    }

    @Ignore
    @Override
    public void testBucketExists() {
    }

    @Ignore
    @Override
    public void testCreateBucketRequest() {
    }

    @Ignore
    @Override
    public void testDeleteBucket() {
    }

    @Ignore
    @Override
    public void testDeleteBucketWithObjects() {
    }

    @Ignore
    @Override
    public void testSetGetBucketAcl() {
    }

    @Ignore
    @Override
    public void testSetBucketAclCanned() {
    }

    @Ignore
    @Override
    public void testSetGetBucketCors() {
    }

    @Ignore
    @Override
    public void testDeleteBucketCors() {
    }

    @Ignore
    @Override
    public void testBucketLifecycle() {
    }

    @Ignore
    @Override
    public void testBucketLocation() {
    }

    @Ignore
    @Override
    public void testSetBucketVersioning() {
    }

    @Ignore
    @Override
    public void testBucketVersions() {
    }

    @Ignore
    @Override
    public void testListObjects() {
    }

    @Ignore
    @Override
    public void testListAndReadVersions() {
    }

    @Ignore
    @Override
    public void testListObjectsWithPrefix() {
    }

    @Ignore
    @Override
    public void testListVersionsPagingPrefixDelim() {
    }

    @Ignore
    @Override
    public void testPutObjectWithMd5() {
    }

    @Ignore
    @Override
    public void testPutObjectWithRetentionPeriod() {
    }

    @Ignore
    @Override
    public void testMpuAbortInMiddle() {
    }

    @Ignore
    @Override
    public void testCopyObjectWithMeta() {
    }

    @Ignore
    @Override
    public void testCreateObjectWithStream() {
    }

    @Ignore
    @Override
    public void testCreateObjectWithRetentionPeriod() {
    }

    @Ignore
    @Override
    public void testCreateObjectWithRetentionPolicy() {
    }

    @Ignore
    @Override
    public void testPutObjectPreconditions() {
    }

    @Ignore
    @Override
    public void testCopyObjectSelf() {
    }

    @Ignore
    @Override
    public void testPreSignedPutUrl() {
    }

    @Ignore
    @Override
    public void testPreSignedPutNoContentType() {
    }

    @Ignore
    @Override
    public void testCreateJsonObjectWithStream() {
    }

    @Ignore
    @Override
    public void testUpdateMetadata() {
    }

    @Ignore
    @Override
    public void testExtendObjectRetentionPeriod() {
    }

    @Ignore
    @Override
    public void testPreSignedUrlHeaderOverrides() throws Exception {
    }

    @Override
    public void testCopyRangeAPI() {
    }

    private class ErrorStream extends FilterInputStream {
        private int callCount = 0;

        ErrorStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public int read(byte[] bytes) throws IOException {
            return read(bytes, 0, bytes.length);
        }

        @Override
        public int read(byte[] bytes, int i, int i1) throws IOException {
            incCallCount();
            return super.read(bytes, i, i1);
        }

        @Override
        public int read() throws IOException {
            incCallCount();
            return super.read();
        }

        private void incCallCount() {
            switch (callCount++) {
                case 0:
                    throw new S3Exception("foo", 500);
            }
        }
    }

    @Override
    protected void assertForListVersionsPaging(int size, int requestCount) {
        Assert.assertEquals("The correct number of versions were NOT returned", 10, size);
        Assert.assertEquals("should be 5 pages", 5, requestCount);
    }
}
