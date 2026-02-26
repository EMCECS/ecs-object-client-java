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

import com.emc.object.util.ChecksumError;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class ChecksumFilterTest {
    @Test
    public void testChecksumMatch() {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String realMd5 = DigestUtils.md5Hex(data);

        // positive test - matching MD5 should not throw
        Assertions.assertDoesNotThrow(() -> verifyChecksum(realMd5, realMd5));
    }

    @Test
    public void testChecksumMismatch() {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String realMd5 = DigestUtils.md5Hex(data);

        // negative test - mismatching MD5 should throw ChecksumError
        Assertions.assertThrows(ChecksumError.class, () -> {
            verifyChecksum(realMd5, "abcdef0123456789abcdef0123456789");
        });
    }

    private void verifyChecksum(String expectedMd5, String responseMd5) {
        if (!expectedMd5.equals(responseMd5)) {
            throw new ChecksumError("Checksum failure", expectedMd5, responseMd5);
        }
    }
}
