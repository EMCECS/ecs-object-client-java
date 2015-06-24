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

import com.emc.codec.encryption.BasicKeyProvider;
import com.emc.codec.encryption.EncryptionConstants;
import com.emc.codec.encryption.EncryptionUtil;
import com.emc.object.EncryptionConfig;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.util.RandomInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.LogMF;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class S3EncryptionClientBasicTest extends S3JerseyClientTest {
    private static final Logger l4j = Logger.getLogger(S3EncryptionClientBasicTest.class);

    protected int keySize = 128;
    protected S3JerseyClient rclient;
    protected S3EncryptionClient eclient;

    private BasicKeyProvider _keyProvider;
    private KeyPair _masterKey;
    private KeyPair _oldKey;

    @Override
    protected String getTestBucketPrefix() {
        return "s3-encryption-client-test";
    }

    @Override
    public void initClient() throws Exception {
        rclient = new S3JerseyClient(createS3Config());
        client = eclient = new S3EncryptionClient(createS3Config(), createEncryptionConfig());
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
        LogMF.debug(l4j, "Master key sizes: public: {} private: {}",
                ((RSAPublicKey) _masterKey.getPublic()).getModulus().bitLength(),
                ((RSAPrivateKey) _masterKey.getPrivate()).getModulus().bitLength());
        _oldKey = EncryptionUtil.rsaKeyPairFromBase64(keyprops.getProperty("oldkey.public"), keyprops.getProperty("oldkey.private"));
        LogMF.debug(l4j, "Old key sizes: public: {} private: {}",
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
    public void testWithUserMeta() throws Exception {
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

    // the following methods aren't supported in the encryption client

    @Ignore
    @Override
    public void testReadObjectStreamRange() throws Exception {
    }

    @Ignore
    @Override
    public void testInitiateListAbortMultipartUploads() throws Exception {
    }

    @Ignore
    @Override
    public void testUpdateObjectWithRange() throws Exception {
    }

    @Ignore
    @Override
    public void testSingleMultipartUploadMostSimpleOnePart() throws Exception {
    }

    @Ignore
    @Override
    public void testSingleMultipartUploadMostSimple() throws Exception {
    }

    @Ignore
    @Override
    public void testSingleMultipartUploadSimple() throws Exception {
    }

    @Ignore
    @Override
    public void testMultiThreadMultipartUploadMostSimple() throws Exception {
    }

    @Ignore
    @Override
    public void testLargeObjectContentLength() throws Exception {
    }

    @Ignore
    @Override
    public void testSingleMultipartUploadListParts() throws Exception {
    }

    @Ignore
    @Override
    public void testLargeFileUploader() throws Exception {
    }

    @Ignore
    @Override
    public void testMultiThreadMultipartUploadListPartsPagination() throws Exception {
    }

    @Ignore
    @Override
    public void testLargeFileDownloader() throws Exception {
    }

    @Ignore
    @Override
    public void testAppendObject() throws Exception {
    }

    // the following methods are unnecessary and/or do not test anything related to encryption


    @Ignore
    @Override
    public void testCreateExistingBucket() throws Exception {
    }

    @Ignore
    @Override
    public void testListBuckets() throws Exception {
    }

    @Ignore
    @Override
    public void testListBucketsReq() {
    }

    @Ignore
    @Override
    public void testBucketExists() throws Exception {
    }

    @Ignore
    @Override
    public void testCreateBucketRequest() throws Exception {
    }

    @Ignore
    @Override
    public void testDeleteBucket() throws Exception {
    }

    @Ignore
    @Override
    public void testDeleteBucketWithObjects() throws Exception {
    }

    @Ignore
    @Override
    public void testSetBucketAcl() throws Exception {
    }

    @Ignore
    @Override
    public void testSetBucketAclCanned() {
    }

    @Ignore
    @Override
    public void testGetBucketCors() throws Exception {
    }

    @Ignore
    @Override
    public void testDeleteBucketCors() throws Exception {
    }

    @Ignore
    @Override
    public void testDeleteBucketLifecycle() throws Exception {
    }

    @Ignore
    @Override
    public void testCreateBucket() throws Exception {
    }

    @Ignore
    @Override
    public void testBucketLocation() throws Exception {
    }

    @Ignore
    @Override
    public void testSetBucketVersioning() throws Exception {
    }

    @Ignore
    @Override
    public void testBucketVersions() throws Exception {
    }
}
