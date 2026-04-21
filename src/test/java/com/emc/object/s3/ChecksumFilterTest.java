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

import java.io.ByteArrayInputStream;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.emc.object.util.ChecksumAlgorithm;
import com.emc.object.util.ChecksumError;
import com.emc.object.util.ChecksumValueImpl;
import com.emc.object.util.ChecksummedInputStream;

public class ChecksumFilterTest {
    @Test
    public void testChecksumVerification() throws Exception {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);

        String correctMd5 = DigestUtils.md5Hex(data);

        // positive test - correct checksum should not throw
        ChecksummedInputStream goodStream = new ChecksummedInputStream(
                new ByteArrayInputStream(data),
                new ChecksumValueImpl(ChecksumAlgorithm.MD5, data.length, correctMd5));
        byte[] buffer = new byte[1024];
        int total = 0, n;
        while ((n = goodStream.read(buffer)) >= 0) total += n;
        goodStream.close();
        Assertions.assertEquals(data.length, total);

        // negative test - bad checksum should throw ChecksumError
        try {
            ChecksummedInputStream badStream = new ChecksummedInputStream(
                    new ByteArrayInputStream(data),
                    new ChecksumValueImpl(ChecksumAlgorithm.MD5, data.length, "abcdef0123456789abcdef0123456789"));
            buffer = new byte[1024];
            while (badStream.read(buffer) >= 0) { /* read to EOF to trigger verification */ }
            badStream.close();
            Assertions.fail("bad MD5 should throw exception");
        } catch (ChecksumError e) {
            // expected
        }
    }
}
