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

import com.emc.object.s3.jersey.ChecksumFilter;
import com.emc.object.util.ChecksumError;
import com.emc.object.util.RestUtil;
import com.sun.jersey.api.client.*;
import com.sun.jersey.core.header.InBoundHeaders;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

public class ChecksumFilterTest {
    @Test
    public void testContentMd5() throws Exception {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);

        MockClientHandler mockHandler = new MockClientHandler();

        Client client = new Client(mockHandler);
        client.addFilter(new ChecksumFilter(new S3Config()));

        // positive test
        mockHandler.setBadMd5(false);
        WebResource resource = client.resource("http://foo.com");
        resource.setProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM, Boolean.TRUE);
        ClientResponse response = resource.put(ClientResponse.class, data);
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());

        try {
            mockHandler.setBadMd5(true);
            resource = client.resource("http://foo.com");
            resource.setProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM, Boolean.TRUE);
            resource.put(ClientResponse.class, data);
            Assert.fail("bad MD5 should throw exception");
        } catch (ChecksumError e) {
            // expected
        }
    }

    // assumes byte[] entity
    class MockClientHandler implements ClientHandler {
        boolean badMd5 = false;

        @Override
        public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
            byte[] content = (byte[]) cr.getEntity();

            // make sure entity is actually written (so digest stream will get real MD5)
            try {
                OutputStream out = cr.getAdapter().adapt(cr, new ByteArrayOutputStream());
                out.write((byte[]) cr.getEntity());
                out.close();
            } catch (IOException e) {
                throw new ClientHandlerException(e);
            }

            // set content MD5 header in response (bad or real)
            InBoundHeaders headers = new InBoundHeaders();
            if (badMd5) headers.add(RestUtil.EMC_CONTENT_MD5, "abcdef0123456789abcdef0123456789");
            else headers.add(RestUtil.EMC_CONTENT_MD5, DigestUtils.md5Hex(content));

            // return mock response with headers and no data
            return new ClientResponse(ClientResponse.Status.OK, headers, new ByteArrayInputStream(new byte[0]), null);
        }

        public void setBadMd5(boolean badMd5) {
            this.badMd5 = badMd5;
        }
    }
}
