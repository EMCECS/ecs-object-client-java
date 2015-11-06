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
        String query = "foo=x&bar=y&baz=z&yo=&alpha=bra%26vo";
        Map<String, String> parameters = RestUtil.getQueryParameterMap(query);

        Assert.assertEquals("x", parameters.get("foo"));
        Assert.assertEquals("y", parameters.get("bar"));
        Assert.assertEquals("z", parameters.get("baz"));
        Assert.assertEquals("", parameters.get("yo"));
        Assert.assertEquals("bra&vo", parameters.get("alpha"));
        Assert.assertEquals(null, parameters.get("bogus"));
    }

    @Test
    public void testReplacePath() throws Exception {
        String host = "http://foo.com";
        String bucket = "foo-bar";
        String key = "foo/[ test & spaces ]/bar";
        String query = "prefix=" + RestUtil.urlEncode("CS_Archive2_Copy/Screens/[ Archived & Toolbox ]/Country Flags");

        S3Config config = new S3Config(new URI(host));
        S3ObjectRequest request = new S3ObjectRequest(Method.GET, bucket, key, null);
        URI uri = config.resolvePath(request.getPath(), query);
        String post = "http://foo.com/foo-bar/foo/%5B%20test%20&%20spaces%20%5D/bar?prefix=CS_Archive2_Copy%2FScreens%2F%5B%20Archived%20%26%20Toolbox%20%5D%2FCountry%20Flags";
        Assert.assertEquals(new URI(post), RestUtil.replacePath(uri, "/" + bucket + "/" + key));
    }

    // Unicode "OHM SYMBOL"
    public static final byte[] OHM_UTF8 = new byte[] { (byte)0xe2, (byte)0x84, (byte)0xa6 };

    /**
     * Tests URI building to make sure that it doesn't modify UTF-8 sequences.  The default URI.toAsciiString runs the
     * path through Unicode "Normalization" that modifies some Unicode characters.  We need to make sure any UTF-8
     * input sequences are the same in and out so object keys are not changed.  In this test specifically, the Ohm
     * symbol below is normalized to a plain Omega symbol, changing the UTF-8 sequence causing ECS to say the object
     * cannot be found.
     */
    @Test
    public void testUnicodeEncode() throws Exception {
        // IntelliJ normalizes Ohm to Omega so you can't paste it as a literal.
        String ohm = new String(OHM_UTF8, "UTF-8");
        String query = "prefix=" + RestUtil.urlEncode("foo/bar/" + OHM_UTF8 + "/baz/");

        URI u = RestUtil.buildUri("http", "www.foo.com", -1, "/100 " + ohm + " Differential impedance 2.rar", query, null);
        Assert.assertEquals("http://www.foo.com/100%20%E2%84%A6%20Differential%20impedance%202.rar?" + query, u.toString());
    }

    private String encodePath(String path) {
        return RestUtil.urlEncode(path).replace("%2F", "/");
    }
}
