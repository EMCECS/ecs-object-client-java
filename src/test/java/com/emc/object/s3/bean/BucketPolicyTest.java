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
package com.emc.object.s3.bean;

import org.apache.commons.codec.Charsets;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class BucketPolicyTest {
    private static String JSON = "{\n" +
            "  \"Id\" : \"PolicyId2\",\n" +
            "  \"Version\" : \"2012-10-17\",\n" +
            "  \"Statement\" : [ {\n" +
            "    \"Sid\" : \"AllowIPmix\",\n" +
            "    \"Effect\" : \"Allow\",\n" +
            "    \"Principal\" : \"*\",\n" +
            "    \"Action\" : [ \"s3:*\" ],\n" +
            "    \"Resource\" : \"arn:aws:s3:::examplebucket/*\",\n" +
            "    \"Condition\" : {\n" +
            "      \"IpAddress\" : {\n" +
            "        \"aws:SourceIp\" : [ \"54.240.143.0/24\", \"2001:DB8:1234:5678::/64\" ]\n" +
            "      },\n" +
            "      \"NotIpAddress\" : {\n" +
            "        \"aws:SourceIp\" : [ \"54.240.143.128/30\", \"2001:DB8:1234:5678:ABCD::/80\" ]\n" +
            "      }\n" +
            "    }\n" +
            "  } ]\n" +
            "}";

    private static BucketPolicy OBJECT = new BucketPolicy().withId("PolicyId2").withVersion("2012-10-17").withStatements(
            new BucketPolicyStatement().withSid("AllowIPmix")
                    .withEffect(BucketPolicyStatement.Effect.Allow)
                    .withPrincipal("*")
                    .withActions(BucketPolicyAction.All)
                    .withResource("arn:aws:s3:::examplebucket/*")
                    .withCondition(PolicyConditionOperator.IpAddress, new PolicyConditionCriteria()
                            .withCondition(PolicyConditionKey.SourceIp, "54.240.143.0/24", "2001:DB8:1234:5678::/64"))
                    .withCondition(PolicyConditionOperator.NotIpAddress, new PolicyConditionCriteria()
                            .withCondition(PolicyConditionKey.SourceIp, "54.240.143.128/30", "2001:DB8:1234:5678:ABCD::/80"))
    );

    @Test
    public void testMarshalling() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationConfig.Feature.INDENT_OUTPUT)
                .setAnnotationIntrospector(AnnotationIntrospector.pair(new JacksonAnnotationIntrospector(), new JaxbAnnotationIntrospector()));

        String generatedJson = mapper.writeValueAsString(OBJECT);
        Assert.assertEquals(JSON, generatedJson);

        Assert.assertEquals(OBJECT, mapper.readValue(JSON, BucketPolicy.class));

        // round trip
        Assert.assertEquals(OBJECT, mapper.readValue(generatedJson, BucketPolicy.class));
    }

    @Test
    public void testProviderMarshalling() throws Exception {
        JacksonJsonProvider provider = new JacksonJaxbJsonProvider();
        // the only difference between this test and the client implementation, is indentation (to ease testing)
        provider.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

        // test writing
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        provider.writeTo(OBJECT, BucketPolicy.class, BucketPolicy.class, null, MediaType.APPLICATION_JSON_TYPE,
                null, baos);

        Assert.assertEquals(JSON, new String(baos.toByteArray(), StandardCharsets.UTF_8));

        // test reading
        provider.readFrom(Object.class, BucketPolicy.class, null, MediaType.APPLICATION_JSON_TYPE,
                null, new ByteArrayInputStream(JSON.getBytes(StandardCharsets.UTF_8)));
    }
}
