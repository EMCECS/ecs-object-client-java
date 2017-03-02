/*
 * Copyright (c) 2015-2016, EMC Corporation.
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

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class ConfigUriTest {
    @Test
    public void testConvertUri() {
        ConfigUri.registerConverter(Date.class, new DateConverter());
        String uri = "https://server.com:1234/pathypathpath?map.foo=bar&dateParam=1475100000000&map.baz=boozle" +
                "&intList=7&intList=8&intList=9&map.bim=bam" +
                "&stringList=bing&stringList=bong&stringList=boozle" +
                "&bar=baz&longParam=6&integerParam=5" +
                "&dateList=1475200000000&dateList=1475300000000&dateList=1475400000000" +
                "&blah=blah";

        ConfigUri<DummyConfig> dummyUri = new ConfigUri<DummyConfig>(DummyConfig.class);

        DummyConfig dummyConfig = dummyUri.parseUri(uri);

        Assert.assertEquals("https", dummyConfig.getProtocol());
        Assert.assertEquals("server.com", dummyConfig.getHost());
        Assert.assertEquals(1234, dummyConfig.getPort());
        Assert.assertEquals("/pathypathpath", dummyConfig.getPath());
        Assert.assertEquals("baz", dummyConfig.getStringParam());
        Assert.assertEquals(new Integer(5), dummyConfig.getIntegerParam());
        Assert.assertEquals(6, dummyConfig.getLongParam());
        Assert.assertEquals(new Date(1475100000000L), dummyConfig.getDateParam());
        Assert.assertEquals(Arrays.asList(new Date(1475200000000L), new Date(1475300000000L), new Date(1475400000000L)),
                dummyConfig.getDateList());
        Assert.assertEquals(Arrays.asList(7, 8, 9), dummyConfig.getIntList());
        Assert.assertEquals(Arrays.asList("bing", "bong", "boozle"), dummyConfig.getStringList());

        Map<String, Foo> fooMap = new HashMap<String, Foo>();
        fooMap.put("foo", Foo.bar);
        fooMap.put("bim", Foo.bam);
        fooMap.put("baz", Foo.boozle);
        Assert.assertEquals(fooMap, dummyConfig.getFooMap());

        // make sure generated URI is the same as the original
        // parameter order may be an issue here, so we'll regenerate
        // this is to test round-trip
        uri = dummyUri.generateUri(dummyConfig) + "&blah=blah";
        dummyConfig = dummyUri.parseUri(uri);
        Assert.assertEquals(uri, dummyUri.generateUri(dummyConfig) + "&blah=blah");

        try {
            dummyUri.parseUri(uri, null, true);
            Assert.fail("invalid parameter should fail with strict parsing");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testUtf8() throws Exception {
        ConfigUri.registerConverter(Date.class, new DateConverter());
        String uri = "https://server.com:1234/path?stringList=fooΩbar&stringList=foo%20bar&stringList=foo服务器bar&bar=foo%20Ω%20bar";

        ConfigUri<DummyConfig> dummyUri = new ConfigUri<DummyConfig>(DummyConfig.class);

        DummyConfig dummyConfig = dummyUri.parseUri(uri);

        Assert.assertEquals("https", dummyConfig.getProtocol());
        Assert.assertEquals("server.com", dummyConfig.getHost());
        Assert.assertEquals(1234, dummyConfig.getPort());
        Assert.assertEquals("/path", dummyConfig.getPath());
        Assert.assertEquals("foo Ω bar", dummyConfig.getStringParam());
        Assert.assertEquals(Arrays.asList("fooΩbar", "foo bar", "foo服务器bar"), dummyConfig.getStringList());

        // make sure generated URI is the same as the original
        // parameter order may be an issue here, so we'll regenerate
        // this is to test round-trip
        uri = dummyUri.generateUri(dummyConfig);
        dummyConfig = dummyUri.parseUri(uri);
        Assert.assertEquals(uri, dummyUri.generateUri(dummyConfig));
    }

    @Test
    public void testBadType() {
        try {
            new ConfigUri<BadPortConfig>(BadPortConfig.class);
            Assert.fail("wrong port type should fail");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testDuplicateType() {
        try {
            new ConfigUri<TooManyHostConfig>(TooManyHostConfig.class);
            Assert.fail("two host properties should fail");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public static class DummyConfig {
        private String protocol;
        private String host;
        private int port;
        private String path;
        private String stringParam;
        private Integer integerParam;
        private long longParam;
        private Date dateParam;
        private List<Date> dateList;
        private List<Integer> intList;
        private List<String> stringList;
        private Map<String, Foo> fooMap;

        @ConfigUriProperty(type = ConfigUriProperty.Type.Protocol)
        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        @ConfigUriProperty(type = ConfigUriProperty.Type.Host)
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        @ConfigUriProperty(type = ConfigUriProperty.Type.Port)
        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @ConfigUriProperty(type = ConfigUriProperty.Type.Path)
        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        @ConfigUriProperty(param = "bar")
        public String getStringParam() {
            return stringParam;
        }

        public void setStringParam(String stringParam) {
            this.stringParam = stringParam;
        }

        @ConfigUriProperty
        public Integer getIntegerParam() {
            return integerParam;
        }

        public void setIntegerParam(Integer integerParam) {
            this.integerParam = integerParam;
        }

        @ConfigUriProperty
        public long getLongParam() {
            return longParam;
        }

        public void setLongParam(long longParam) {
            this.longParam = longParam;
        }

        @ConfigUriProperty
        public Date getDateParam() {
            return dateParam;
        }

        public void setDateParam(Date dateParam) {
            this.dateParam = dateParam;
        }

        @ConfigUriProperty(converter = DateConverter.class)
        public List<Date> getDateList() {
            return dateList;
        }

        public void setDateList(List<Date> dateList) {
            this.dateList = dateList;
        }

        @ConfigUriProperty(converter = ConfigUri.IntegerPropertyConverter.class)
        public List<Integer> getIntList() {
            return intList;
        }

        public void setIntList(List<Integer> intList) {
            this.intList = intList;
        }

        @ConfigUriProperty(converter = ConfigUri.StringPropertyConverter.class)
        public List<String> getStringList() {
            return stringList;
        }

        public void setStringList(List<String> stringList) {
            this.stringList = stringList;
        }

        @ConfigUriProperty(param = "map", converter = FooConverter.class)
        public Map<String, Foo> getFooMap() {
            return fooMap;
        }

        public void setFooMap(Map<String, Foo> fooMap) {
            this.fooMap = fooMap;
        }
    }

    public static class BadPortConfig {
        private String port;

        @ConfigUriProperty(type = ConfigUriProperty.Type.Port)
        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }
    }

    public static class TooManyHostConfig {
        private String host1;
        private String host2;

        @ConfigUriProperty(type = ConfigUriProperty.Type.Host)
        public String getHost1() {
            return host1;
        }

        public void setHost1(String host1) {
            this.host1 = host1;
        }

        @ConfigUriProperty(type = ConfigUriProperty.Type.Host)
        public String getHost2() {
            return host2;
        }

        public void setHost2(String host2) {
            this.host2 = host2;
        }
    }

    public static class DateConverter implements ConfigUri.PropertyConverter {
        @Override
        public Object valueFromString(String param) {
            if (param == null) return null;
            return new Date(Long.parseLong(param));
        }

        @Override
        public String stringFromValue(Object value) {
            if (value == null) return null;
            return "" + ((Date) value).getTime();
        }
    }

    public static class FooConverter implements ConfigUri.PropertyConverter {
        @Override
        public Object valueFromString(String param) {
            if (param == null) return null;
            return Foo.valueOf(param);
        }

        @Override
        public String stringFromValue(Object value) {
            if (value == null) return null;
            return value.toString();
        }
    }

    public enum Foo {
        bar, bam, boozle
    }
}
