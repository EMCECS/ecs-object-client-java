package com.emc.object.s3;

import com.emc.codec.encryption.EncryptionConstants;
import com.emc.codec.encryption.EncryptionUtil;
import com.emc.codec.encryption.KeystoreKeyProvider;
import com.emc.object.EncryptionConfig;
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.jersey.S3JerseyClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

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
        Assertions.assertFalse(eclient.rekey(getTestBucket(), key));

        // change master key
        EncryptionConfig encryptionConfig = createEncryptionConfig();
        ((KeystoreKeyProvider) getKeyProvider()).setMasterKeyAlias(oldKeyAlias);
        S3EncryptionClient eclient2 = new S3EncryptionClient(createS3Config().withUseV2Signer(false), encryptionConfig);

        // now actually rekey
        Assertions.assertTrue(eclient2.rekey(getTestBucket(), key));

        // Read back and test
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);

        KeyPair oldKeyPair = ((KeystoreKeyProvider) getKeyProvider()).getKeyFromAlias(oldKeyAlias);
        String oldKeyFingerprint = EncryptionUtil.getRsaPublicKeyFingerprint((RSAPublicKey) oldKeyPair.getPublic());
        Assertions.assertEquals(content, client.readObject(getTestBucket(), key, String.class), "Content differs");
        Assertions.assertEquals("12",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SIZE), "unencrypted size incorrect");
        Assertions.assertEquals(16, objectMetadata.getContentLength().longValue(), "encrypted size incorrect");
        Assertions.assertEquals("2ef7bde608ce5404e97d5f042f95f89f1c232871",
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1), "unencrypted sha1 incorrect");
        Assertions.assertEquals(oldKeyFingerprint,
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_KEY_ID), "master key ID incorrect");
        Assertions.assertNotNull(objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_IV), "IV null");
        Assertions.assertNotNull(objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_OBJECT_KEY), "Object key");
        Assertions.assertNotNull(
                objectMetadata.getUserMetadata(EncryptionConstants.META_ENCRYPTION_META_SIG), "Missing metadata signature");
    }
}
