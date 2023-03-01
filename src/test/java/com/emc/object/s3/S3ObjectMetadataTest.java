/*
 * Copyright (c) 2015-2018, EMC Corporation.
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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class S3ObjectMetadataTest {
    @Test
    public void testCaseInsensitivity() {
        S3ObjectMetadata metadata = new S3ObjectMetadata()
                .addUserMetadata("One", "Two")
                .addUserMetadata("one", "three")
                .addUserMetadata("ONE", "FOUR")

                .addUserMetadata("Five", "Six")
                .addUserMetadata("five", "seven")
                .addUserMetadata("FIVE", "EIGHT");

        // TreeMap will sort the keys and the first insertion case is preserved
        Assertions.assertArrayEquals(new String[]{"Five", "One"}, metadata.getUserMetadata().keySet().toArray());
        Assertions.assertEquals("FOUR", metadata.getUserMetadata("one"));
        Assertions.assertEquals("FOUR", metadata.getUserMetadata("One"));
        Assertions.assertEquals("FOUR", metadata.getUserMetadata("oNe"));
        Assertions.assertEquals("EIGHT", metadata.getUserMetadata("five"));
        Assertions.assertEquals("EIGHT", metadata.getUserMetadata("Five"));
        Assertions.assertEquals("EIGHT", metadata.getUserMetadata("fIve"));
    }
}
