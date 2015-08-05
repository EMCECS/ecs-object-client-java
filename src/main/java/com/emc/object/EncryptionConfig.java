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

import com.emc.codec.compression.deflate.DeflateCodec;
import com.emc.codec.encryption.BasicKeyProvider;
import com.emc.codec.encryption.EncryptionCodec;
import com.emc.codec.encryption.KeyProvider;
import com.emc.codec.encryption.KeystoreKeyProvider;

import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates an encryption configuration for use with {@link com.emc.object.s3.jersey.S3EncryptionClient}.
 * Both keystore keys and bare RSA KeyPairs are supported. You can optionally implement your own
 * {@link KeyProvider} as well.
 */
public class EncryptionConfig {
    // NOTE: if you add a property, make sure you add it to the cloning constructor!
    private String encryptionSpec = new EncryptionCodec().getDefaultEncodeSpec();
    private boolean compressionEnabled = false;
    private String compressionSpec = new DeflateCodec().getDefaultEncodeSpec();
    private Map<String, Object> codecProperties = new HashMap<String, Object>();

    /**
     * Creates a new EncryptionConfig object that will retrieve keys from a Keystore
     * object. Note that currently, only RSA keys are supported.
     *
     * @param keystore          the Keystore containing the master encryption key and any
     *                          additional decryption key(s).
     * @param masterKeyPassword password for the master keys.  Note that this
     *                          implementation assumes that all master keys use the same password.
     * @param masterKeyAlias    name of the master encryption key in the Keystore object.
     * @throws java.security.KeyStoreException              if the keystore has not been initialized properly.
     * @throws java.security.NoSuchAlgorithmException       if the master key's algorithm is not available.
     * @throws java.security.InvalidKeyException            if the master key alias is not found in the keystore.
     * @throws java.security.UnrecoverableKeyException      if the master key is not readable (e.g. bad password)
     */
    public EncryptionConfig(KeyStore keystore, char[] masterKeyPassword, String masterKeyAlias)
            throws KeyStoreException, NoSuchAlgorithmException, InvalidKeyException, UnrecoverableKeyException {
        this(new KeystoreKeyProvider(keystore, masterKeyPassword, masterKeyAlias));
    }

    /**
     * Creates a new EncryptionConfig object that uses bare KeyPair objects.
     *
     * @param masterEncryptionKey the KeyPair to use for encryption.
     * @param decryptionKeys      (optional) additional KeyPair objects available to
     *                            decrypt objects.
     */
    public EncryptionConfig(KeyPair masterEncryptionKey, Set<KeyPair> decryptionKeys) {
        this(new BasicKeyProvider(masterEncryptionKey, decryptionKeys.toArray(new KeyPair[decryptionKeys.size()])));
    }

    public EncryptionConfig(KeyProvider keyProvider) {
        codecProperties.put(EncryptionCodec.PROP_KEY_PROVIDER, keyProvider);
    }

    /**
     * Cloning constructor.
     */
    public EncryptionConfig(EncryptionConfig other) {
        this.encryptionSpec = other.encryptionSpec;
        this.compressionEnabled = other.compressionEnabled;
        this.compressionSpec = other.compressionSpec;
        this.codecProperties = new HashMap<String, Object>(codecProperties);
    }

    public KeyProvider getKeyProvider() {
        return EncryptionCodec.getKeyProvider(codecProperties);
    }

    public int getKeySize() {
        return EncryptionCodec.getKeySize(codecProperties);
    }

    /**
     * Set the size of encryption key to use, either 128 or 256.  Note that to use
     * 256-bit AES keys, you will probably need the unlimited strength jurisdiction files
     * installed in your JRE.
     */
    public void setKeySize(int keySize) {
        EncryptionCodec.setKeySize(codecProperties, keySize);
    }

    public Provider getSecurityProvider() {
        return EncryptionCodec.getSecurityProvider(codecProperties);
    }

    /**
     * Set the Java security provider to use for all encryption operations.  If not specified,
     * the default provider(s) will be used from your java.security file.
     */
    public void setSecurityProvider(Provider provider) {
        EncryptionCodec.setPropSecurityProvider(codecProperties, provider);
    }

    public String getEncryptionSpec() {
        return encryptionSpec;
    }

    public void setEncryptionSpec(String encryptionSpec) {
        this.encryptionSpec = encryptionSpec;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    /**
     * If set to true, will enable compression for objects created with the encryption client.
     */
    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public String getCompressionSpec() {
        return compressionSpec;
    }

    /**
     * Sets the compression spec to use for compression. Defaults to Deflate, level 5.
     *
     * @see DeflateCodec#encodeSpec(int)
     * @see com.emc.codec.compression.lzma.LzmaCodec#encodeSpec(int)
     */
    public void setCompressionSpec(String compressionSpec) {
        this.compressionSpec = compressionSpec;
    }

    public Map<String, Object> getCodecProperties() {
        return codecProperties;
    }

    public void setCodecProperties(Map<String, Object> codecProperties) {
        this.codecProperties = codecProperties;
    }

    /**
     * @see #setKeySize(int)
     */
    public EncryptionConfig withKeySize(int keySize) {
        setKeySize(keySize);
        return this;
    }

    /**
     * @see #setSecurityProvider(Provider)
     */
    public EncryptionConfig withSecurityProvider(Provider provider) {
        setSecurityProvider(provider);
        return this;
    }

    public EncryptionConfig withEncryptionSpec(String encryptionSpec) {
        setEncryptionSpec(encryptionSpec);
        return this;
    }

    public EncryptionConfig withCompressionEnabled(boolean compressionEnabled) {
        setCompressionEnabled(compressionEnabled);
        return this;
    }

    public EncryptionConfig withCompressionSpec(String compressionSpec) {
        setCompressionSpec(compressionSpec);
        return this;
    }
}
