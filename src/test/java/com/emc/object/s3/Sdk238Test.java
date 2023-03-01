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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.net.URI;

public class Sdk238Test {
    @Test
    public void testTrailingSlash() throws Exception {
        S3Config s3Config = AbstractS3ClientTest.s3ConfigFromProperties();
        TestClient client = new TestClient(s3Config);

        String bucket = "test-trailing-slash";
        client.createBucket(bucket);
        try {
            if (s3Config.isUseVHost()) {
                Assertions.assertEquals("/", client.getLastUri().getPath());
            } else {
                Assertions.assertEquals("/" + bucket, client.getLastUri().getPath());
            }
        } finally {
            client.deleteBucket(bucket);
        }
    }

    private static class TestClient extends S3JerseyClient {
        private final UriCaptureFilter captureFilter = new UriCaptureFilter();

        TestClient(S3Config s3Config) {
            super(s3Config);
        }

        URI getLastUri() {
            return captureFilter.getLastUri();
        }
    }

    protected static class UriCaptureFilter implements ClientRequestFilter {
        private URI uri;

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            uri = requestContext.getUri();
        }

        URI getLastUri() {
            return uri;
        }
    }
}
