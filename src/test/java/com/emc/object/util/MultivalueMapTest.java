package com.emc.object.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class MultivalueMapTest {
    @Test
    public void testDelimitedValue() {
        MultivalueMap headers = new MultivalueMap();

        headers.putSingle("foo", "a");
        Assert.assertEquals("a", headers.getDelimited("foo", ","));

        headers.put("bar", Arrays.asList((Object) "x", "y", "z"));
        Assert.assertEquals("x,y,z", headers.getDelimited("bar", ","));

        headers.putSingle("baz", "yo");
        headers.addValue("baz", "da");
        Assert.assertEquals("yo,da", headers.getDelimited("baz", ","));

        // single empty string
        headers.putSingle("alpha", "");
        Assert.assertEquals("", headers.getDelimited("alpha", ","));

        // single null
        headers.putSingle("bravo", null);
        Assert.assertEquals("null", headers.getDelimited("bravo", ","));

        // string followed by null
        headers.putSingle("charlie", "c");
        headers.addValue("charlie", null);
        Assert.assertEquals("c,null", headers.getDelimited("charlie", ","));

        // string followed by empty
        headers.putSingle("delta", "d");
        headers.addValue("delta", "");
        Assert.assertEquals("d,", headers.getDelimited("delta", ","));

        // null followed by string
        headers.putSingle("echo", null);
        headers.addValue("echo", "e");
        Assert.assertEquals("null,e", headers.getDelimited("echo", ","));

        // empty followed by string
        headers.putSingle("foxtrot", "");
        headers.addValue("foxtrot", "f");
        Assert.assertEquals(",f", headers.getDelimited("foxtrot", ","));

        // null in middle
        headers.putSingle("golf", "g");
        headers.addValue("golf", null);
        headers.addValue("golf", "x");
        Assert.assertEquals("g,null,x", headers.getDelimited("golf", ","));

        // empty in middle
        headers.putSingle("hotel", "h");
        headers.addValue("hotel", "");
        headers.addValue("hotel", "x");
        Assert.assertEquals("h,,x", headers.getDelimited("hotel", ","));

        // spaces
        headers.putSingle("india", " ");
        headers.addValue("india", "   ");
        headers.addValue("india", " ");
        Assert.assertEquals(" ,   , ", headers.getDelimited("india", ","));
    }
}
