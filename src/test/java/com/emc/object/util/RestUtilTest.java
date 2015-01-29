package com.emc.object.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class RestUtilTest {
    @Test
    public void testQueryMap() {
        String query = "";
        Map<String, String> parameters = RestUtil.getQueryParameterMap(query);
        Assert.assertEquals(0, parameters.size());

        query = "   ";
        parameters = RestUtil.getQueryParameterMap(query);
        Assert.assertEquals(0, parameters.size());

        query = "foo=bar&baz=foo";
        parameters = RestUtil.getQueryParameterMap(query);
        Assert.assertEquals(2, parameters.size());
        Assert.assertEquals("bar", parameters.get("foo"));
        Assert.assertEquals("foo", parameters.get("baz"));

        query = "alpha=&bravo=charlie";
        parameters = RestUtil.getQueryParameterMap(query);
        Assert.assertEquals(2, parameters.size());
        Assert.assertEquals("", parameters.get("alpha"));
        Assert.assertEquals("charlie", parameters.get("bravo"));

        // you don't need an equals sign (value will be null)
        query = "delta=echo&foxtrot";
        parameters = RestUtil.getQueryParameterMap(query);
        Assert.assertEquals(2, parameters.size());
        Assert.assertEquals("echo", parameters.get("delta"));
        Assert.assertEquals(null, parameters.get("foxtrot"));

        // ampersand at the end is ignored
        query = "golf=hotel&";
        parameters = RestUtil.getQueryParameterMap(query);
        Assert.assertEquals(1, parameters.size());
        Assert.assertEquals("hotel", parameters.get("golf"));

        // negative tests

        query = "&a=b";
        try {
            RestUtil.getQueryParameterMap(query);
            Assert.fail("ampersand at the beginning is bad (no empty parameters)");
        } catch (IllegalArgumentException e) {
            // expected
        }

        query = "a=&&b=c";
        try {
            RestUtil.getQueryParameterMap(query);
            Assert.fail("consecutive ampersands are bad (no empty parameters)");
        } catch (IllegalArgumentException e) {
            // expected
        }

        query = "a=b& ";
        try {
            RestUtil.getQueryParameterMap(query);
            Assert.fail("a space is not a key");
        } catch (IllegalArgumentException e) {
            // expected
        }

        query = "a=b& =foo";
        try {
            RestUtil.getQueryParameterMap(query);
            Assert.fail("a space is not a key");
        } catch (IllegalArgumentException e) {
            // expected
        }

        query = "=d";
        try {
            RestUtil.getQueryParameterMap(query);
            Assert.fail("empty key should fail");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testQueryValue() {
        String query = "foo=x&bar=y&baz=z&yo=&alpha=bravo";
        Map<String, String> parameters = RestUtil.getQueryParameterMap(query);

        Assert.assertEquals("x", parameters.get("foo"));
        Assert.assertEquals("y", parameters.get("bar"));
        Assert.assertEquals("z", parameters.get("baz"));
        Assert.assertEquals("", parameters.get("yo"));
        Assert.assertEquals("bravo", parameters.get("alpha"));
        Assert.assertEquals(null, parameters.get("bogus"));
    }
}
