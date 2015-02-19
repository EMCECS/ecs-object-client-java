/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
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
package com.emc.object.util;

import com.emc.rest.util.StreamUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class InputStreamSegmentTest {
    @Test
    public void testMiddle() throws Exception {
        String s = "0123456789Hello Middle!3456789";
        //                    1         2

        InputStream is = new InputStreamSegment(new ByteArrayInputStream(s.getBytes("UTF-8")), 10, 13);

        // read entire stream
        String result = StreamUtil.readAsString(is);
        Assert.assertEquals("Hello Middle!", result);
    }

    @Test
    public void testBeginning() throws Exception {
        String s = "Hello Middle!34567890123456789";
        //                    1         2

        InputStream is = new InputStreamSegment(new ByteArrayInputStream(s.getBytes("UTF-8")), 0, 13);

        // read entire stream
        String result = StreamUtil.readAsString(is);
        Assert.assertEquals("Hello Middle!", result);
    }

    @Test
    public void testEnd() throws Exception {
        String s = "01234567890123456Hello Middle!";
        //                    1         2

        InputStream is = new InputStreamSegment(new ByteArrayInputStream(s.getBytes("UTF-8")), 17, 13);

        // read entire stream
        String result = StreamUtil.readAsString(is);
        Assert.assertEquals("Hello Middle!", result);
    }
}
