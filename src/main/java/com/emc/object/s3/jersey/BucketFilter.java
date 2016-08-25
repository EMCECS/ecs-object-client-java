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
package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Constants;
import com.emc.object.util.RestUtil;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class BucketFilter extends ClientFilter {

    private static final Logger log = LoggerFactory.getLogger(BucketFilter.class);

    public static URI insertBucket(URI uri, String bucketName, boolean useVHost) {
        try {
            if (useVHost) { // prepend to hostname (i.e. bucket.s3.company.com)
                String hostname = bucketName + "." + uri.getHost();
                uri = RestUtil.replaceHost(uri, hostname);

            } else { // prepend to resource path (i.e. s3.company.com/bucket)
                String resource = "/" + bucketName;
                if (!uri.getPath().isEmpty() && !"/".equals(uri.getPath())) resource += uri.getPath();
                uri = RestUtil.replacePath(uri, resource);
            }

            log.debug("URI including bucket: " + uri);
            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("bucket name \"%s\" generated an invalid URI", bucketName), e);
        }
    }

    private S3Config s3Config;

    public BucketFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        URI uri = request.getURI();

        String bucketName = (String) request.getProperties().get(S3Constants.PROPERTY_BUCKET_NAME);
        if (bucketName != null) {
            request.setURI(insertBucket(uri, bucketName, s3Config.isUseVHost()));
        }

        return getNext().handle(request);
    }
}
