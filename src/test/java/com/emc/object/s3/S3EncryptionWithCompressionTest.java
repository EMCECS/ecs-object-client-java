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
import com.emc.codec.compression.CompressionConstants;
import com.emc.codec.encryption.EncryptionConstants;
import com.emc.object.EncryptionConfig;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.object.util.ChecksumAlgorithm;
import com.emc.object.util.ChecksummedInputStream;
import com.emc.object.util.RunningChecksum;
import com.emc.rest.util.StreamUtil;
import com.emc.util.RandomInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assume;
import org.junit.Ignore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class S3EncryptionWithCompressionTest extends S3EncryptionClientBasicTest {
    @Override
    protected EncryptionConfig createEncryptionConfig() throws Exception {
        return super.createEncryptionConfig().withCompressionEnabled(true);
    }

    @Override
    public void testEncodeMeta() throws Exception {
        String key = "hello.txt";
        String content = "Hello World!";

        client.putObject(getTestBucket(), key, content, null);
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);
        Map<String, String> encodedMetadata = objectMetadata.getUserMetadata();

        // manually deflate
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(CompressionConstants.DEFAULT_COMPRESSION_LEVEL));
        dos.write(content.getBytes("UTF-8"));
        dos.close();
        byte[] deflatedData = baos.toByteArray();

        assertEquals(32, objectMetadata.getContentLength().longValue());

        assertEquals("original digest incorrect", DigestUtils.sha1Hex(content.getBytes("UTF-8")),
                encodedMetadata.get(CompressionConstants.META_COMPRESSION_UNCOMP_SHA1));
        assertEquals("Uncompressed size incorrect", 12,
                Long.parseLong(encodedMetadata.get(CompressionConstants.META_COMPRESSION_UNCOMP_SIZE)));
        assertEquals("Compression ratio incorrect", "-66.7%",
                encodedMetadata.get(CompressionConstants.META_COMPRESSION_COMP_RATIO));
        assertEquals("Compressed size incorrect", deflatedData.length,
                Long.parseLong(encodedMetadata.get(CompressionConstants.META_COMPRESSION_COMP_SIZE)));

        assertEquals("Unencrypted digest incorrect", DigestUtils.sha1Hex(deflatedData),
                encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("Unencrypted size incorrect", deflatedData.length,
                Long.parseLong(encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_UNENC_SIZE)));
        assertNotNull("Missing IV", encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_IV));
        assertEquals("Incorrect master encryption key ID", getKeyProvider().getMasterKeyFingerprint(),
                encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_KEY_ID));
        assertNotNull("Missing object key", encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_OBJECT_KEY));
        assertNotNull("Missing metadata signature", encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_META_SIG));

        assertEquals("Transform config string incorrect", "COMP:Deflate/5,ENC:AES/CBC/PKCS5Padding",
                encodedMetadata.get(CodecChain.META_TRANSFORM_MODE));
    }

    @Override
    public void testStream() throws Exception {
        String key = "test-file.txt";
        InputStream rawInput = getClass().getClassLoader().getResourceAsStream("uncompressed.txt");
        Assume.assumeNotNull(rawInput);

        client.putObject(new PutObjectRequest(getTestBucket(), key, rawInput)
                .withObjectMetadata(new S3ObjectMetadata().withContentLength(2516125L)));
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);
        Map<String, String> encodedMetadata = objectMetadata.getUserMetadata();

        // manually deflate
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(CompressionConstants.DEFAULT_COMPRESSION_LEVEL));
        StreamUtil.copy(getClass().getClassLoader().getResourceAsStream("uncompressed.txt"), dos, 2516125);
        dos.close();
        byte[] deflatedData = baos.toByteArray();

        assertEquals(223552, objectMetadata.getContentLength().longValue());

        assertEquals("original digest incorrect", "027e997e6b1dfc97b93eb28dc9a6804096d85873",
                encodedMetadata.get(CompressionConstants.META_COMPRESSION_UNCOMP_SHA1));
        assertEquals("Uncompressed size incorrect", 2516125,
                Long.parseLong(encodedMetadata.get(CompressionConstants.META_COMPRESSION_UNCOMP_SIZE)));
        assertEquals("Compression ratio incorrect", "91.1%",
                encodedMetadata.get(CompressionConstants.META_COMPRESSION_COMP_RATIO));
        assertEquals("Compressed size incorrect", deflatedData.length,
                Long.parseLong(encodedMetadata.get(CompressionConstants.META_COMPRESSION_COMP_SIZE)));

        assertEquals("Unencrypted digest incorrect", DigestUtils.sha1Hex(deflatedData),
                encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_UNENC_SHA1));
        assertEquals("Unencrypted size incorrect", deflatedData.length,
                Long.parseLong(encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_UNENC_SIZE)));
        assertNotNull("Missing IV", encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_IV));
        assertEquals("Incorrect master encryption key ID", getKeyProvider().getMasterKeyFingerprint(),
                encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_KEY_ID));
        assertNotNull("Missing object key", encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_OBJECT_KEY));
        assertNotNull("Missing metadata signature", encodedMetadata.get(EncryptionConstants.META_ENCRYPTION_META_SIG));

        assertEquals("Transform config string incorrect", "COMP:Deflate/5,ENC:AES/CBC/PKCS5Padding",
                encodedMetadata.get(CodecChain.META_TRANSFORM_MODE));
    }

    @Override
    public void testLargeStream() throws Exception {
        String key = "big-stream.obj";
        int size = 5 * 1024 * 1024 + 13;
        RandomInputStream rs = new RandomInputStream(size);
        ChecksummedInputStream cis = new ChecksummedInputStream(rs, new RunningChecksum(ChecksumAlgorithm.SHA1));

        client.putObject(new PutObjectRequest(getTestBucket(), key, cis)
                .withObjectMetadata(new S3ObjectMetadata().withContentLength((long) size)));
        S3ObjectMetadata objectMetadata = rclient.getObjectMetadata(getTestBucket(), key);

        // Make sure the checksum matches
        String sha1hex = cis.getChecksum().getValue();

        assertNotNull("Missing SHA1 meta", objectMetadata.getUserMetadata(CompressionConstants.META_COMPRESSION_UNCOMP_SHA1));
        assertEquals("SHA1 incorrect", sha1hex,
                objectMetadata.getUserMetadata(CompressionConstants.META_COMPRESSION_UNCOMP_SHA1));
        assertEquals("Stream length incorrect", size,
                Integer.parseInt(objectMetadata.getUserMetadata(CompressionConstants.META_COMPRESSION_UNCOMP_SIZE)));
    }

    @Ignore
    @Override
    public void testRekey() throws Exception {
    }
}
