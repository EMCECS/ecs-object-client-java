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
package com.emc.object.util;

import com.emc.object.Method;
import com.emc.object.s3.S3Config;
import com.emc.object.s3.request.S3ObjectRequest;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
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

    @Test
    public void testReplacePath() throws Exception {
        String host = "http://foo.com";
        String bucket = "foo-bar";
        String key = "foo/[ test spaces ]/bar";
        String query = "prefix=CS_Archive2_Copy/Screens/[ Archived Toolbox ]/Country Flags";

        S3Config config = new S3Config(new URI(host));
        S3ObjectRequest request = new S3ObjectRequest(Method.GET, bucket, key, null);
        URI uri = config.resolvePath(request.getPath(), query);
        String post = "http://foo.com/foo-bar/foo/%5B%20test%20spaces%20%5D/bar?prefix=CS_Archive2_Copy/Screens/%5B%20Archived%20Toolbox%20%5D/Country%20Flags";
        Assert.assertEquals(new URI(post), RestUtil.replacePath(uri, "/" + bucket + "/" + key));
    }

    private String encodePath(String path) {
        return RestUtil.urlEncode(path).replace("%2F", "/");
    }
}
