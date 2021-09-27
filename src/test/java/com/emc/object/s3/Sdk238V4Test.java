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
package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sdk238V4Test extends Sdk238Test{
    @Override
    @Test
    public void testTrailingSlash() throws Exception {
        Sdk238V4Test.TestClient client = new Sdk238V4Test.TestClient(AbstractS3ClientTest.s3ConfigFromProperties());

        String bucket = "test-trailing-slash-v4";
        client.createBucket(bucket);
        try {
            Assert.assertEquals("/" + bucket, client.getLastUri().getPath());
        } finally {
            client.deleteBucket(bucket);
        }
    }

    private class TestClient extends S3JerseyClient {
        private UriCaptureFilter captureFilter = new UriCaptureFilter();

        TestClient(S3Config s3Config) {
            super(s3Config.withUseV2Signer(false));

            List<ClientFilter> filters = new ArrayList<ClientFilter>();

            ClientHandler handler = client.getHeadHandler();
            while (handler instanceof ClientFilter) {
                ClientFilter filter = (ClientFilter) handler;
                filters.add(filter);
                handler = filter.getNext();
            }

            filters.add(captureFilter);

            Collections.reverse(filters);
            client.removeAllFilters();
            for (ClientFilter filter : filters) {
                client.addFilter(filter);
            }
        }

        URI getLastUri() {
            return captureFilter.getLastUri();
        }
    }
}
