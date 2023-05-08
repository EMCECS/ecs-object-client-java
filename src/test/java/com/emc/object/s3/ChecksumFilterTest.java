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

import com.emc.object.s3.jersey.ChecksumRequestFilter;
import com.emc.object.s3.jersey.ChecksumResponseFilter;
import com.emc.object.s3.jersey.FilterPriorities;
import com.emc.object.util.ChecksumError;
import com.emc.object.util.RestUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Priority;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

public class ChecksumFilterTest {
    @Test
    public void testContentMd5() throws Exception {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);

        MockClientHandler mockHandler = new MockClientHandler();

        JerseyClient client = JerseyClientBuilder.createClient();
        client.register(mockHandler);
        client.register(new ChecksumRequestFilter(new S3Config()));
        client.register(new ChecksumResponseFilter());

        // positive test
        mockHandler.setBadMd5(false);
        JerseyWebTarget resource = client.target("http://foo.com");
        resource.property(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM, Boolean.TRUE);
        Response response = resource.request().put(Entity.entity(data, RestUtil.DEFAULT_CONTENT_TYPE));
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        response.close();

        try {
            mockHandler.setBadMd5(true);
            resource = client.target("http://foo.com");
            resource.property(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM, Boolean.TRUE);
            resource.request().put(Entity.entity(data, RestUtil.DEFAULT_CONTENT_TYPE));
            Assert.fail("bad MD5 should throw exception");
        } catch (ProcessingException e) {
            Assert.assertTrue(e.getCause() instanceof ChecksumError);
        }
        client.close();
    }

    @Provider
    @Priority(FilterPriorities.PRIORITY_CHECKSUM_RESPONSE + 1)
    class MockClientHandler implements ClientResponseFilter {
        boolean badMd5 = false;

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            byte[] content = (byte[]) requestContext.getEntity();
            // set content MD5 header in response (bad or real)
            String MD5 = badMd5 ? "abcdef0123456789abcdef0123456789" : DigestUtils.md5Hex(content);
            // return mock response with headers and no data
            responseContext.setStatus(Response.Status.OK.getStatusCode());
            responseContext.getHeaders().putSingle(RestUtil.EMC_CONTENT_MD5, MD5);
            responseContext.setEntityStream(new ByteArrayInputStream(new byte[0]));
        }

        public void setBadMd5(boolean badMd5) {
            this.badMd5 = badMd5;
        }

    }
}
