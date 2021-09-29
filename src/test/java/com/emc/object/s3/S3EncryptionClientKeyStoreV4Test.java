package com.emc.object.s3;

import com.emc.codec.encryption.EncryptionConstants;
import com.emc.codec.encryption.EncryptionUtil;
import com.emc.codec.encryption.KeystoreKeyProvider;
import com.emc.object.EncryptionConfig;
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.jersey.S3JerseyClient;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import static org.junit.Assert.assertEquals;

public class S3EncryptionClientKeyStoreV4Test extends S3EncryptionClientKeyStoreTest {
    private static final Logger log = LoggerFactory.getLogger(S3EncryptionClientKeyStoreV4Test.class);

    private String oldKeyAlias = "oldkey";

    @Override
    protected String getTestBucketPrefix() {
        return "s3-encryption-client-keystore-v4-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        rclient = new S3JerseyClient(createS3Config().withUseV2Signer(false));
        EncryptionConfig eConfig = createEncryptionConfig();
        eclient = new S3EncryptionClient(createS3Config().withUseV2Signer(false), eConfig);
        encodeSpec = eConfig.getEncryptionSpec();
        if (eConfig.isCompressionEnabled()) encodeSpec = eConfig.getCompressionSpec() + "," + encodeSpec;
        return eclient;
    }

    @Override
    @Test
    public void testRekey() throws Exception {
        String key = "rekey-test.txt";
        String content = "Hello World!";

        client.putObject(getTestBucket(), key, content, null);

        // shouldn't need to rekey as the master key has not changed
        Assert.assertFalse(eclient.rekey(getTestBucket(), key));

        // change master key
        EncryptionConfig encryptionConfig = createEncryptionConfig();
        ((KeystoreKeyProvider) getKeyProvider()).setMasterKeyAlias(oldKeyAlias);
        S3EncryptionClient eclient2 = new S3EncryptionClient(createS3Config().withUseV2Signer(false), encryptionConfig);

        // now actually rekey
        Assert.assertTrue(eclient2.rekey(getTestBucket(), key));

        // Read back and test
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);

        KeyPair oldKeyPair = ((KeystoreKeyProvider) getKeyProvider()).getKeyFromAlias(oldKeyAlias);
        String oldKeyFingerprint = EncryptionUtil.getRsaPublicKeyFingerprint((RSAPublicKey) oldKeyPair.getPublic());
        assertEquals("Content differs", content, client.readObject(getTestBucket(), key, String.class));
        assertEquals("unencrypted size incorrect", "12",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SIZE));
        assertEquals("encrypted size incorrect", 16, objectMetadata.getContentLength().longValue());
        assertEquals("unencrypted sha1 incorrect", "2ef7bde608ce5404e97d5f042f95f89f1c232871",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("master key ID incorrect", oldKeyFingerprint,
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_KEY_ID));
        Assert.assertNotNull("IV null", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_IV));
        Assert.assertNotNull("Object key", objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_OBJECT_KEY));
        Assert.assertNotNull("Missing metadata signature",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_META_SIG));
    }
}
