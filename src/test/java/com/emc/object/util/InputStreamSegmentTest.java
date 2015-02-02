/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.util;

import com.emc.util.StreamUtil;
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
