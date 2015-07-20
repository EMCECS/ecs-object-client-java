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
package com.emc.object.s3.jersey;

import com.emc.object.s3.S3AuthUtil;
import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Constants;
import com.emc.object.util.RestUtil;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import java.util.Map;

public class AuthorizationFilter extends ClientFilter {
    private S3Config s3Config;

    public AuthorizationFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {

        // tack on user-agent here
        if (s3Config.getUserAgent() != null)
            request.getHeaders().putSingle(RestUtil.HEADER_USER_AGENT, s3Config.getUserAgent());

        // if no identity is provided, this is an anonymous client
        if (s3Config.getIdentity() != null) {
            Map<String, String> parameters = RestUtil.getQueryParameterMap(request.getURI().getQuery());
            String resource = RestUtil.getEncodedPath(request.getURI());

            // check if bucket is in hostname
            if (s3Config.isUseVHost()) {
                String bucketName = (String) request.getProperties().get(S3Constants.PROPERTY_BUCKET_NAME);
                if (bucketName != null) resource = "/" + bucketName + resource;
            }

            // check if namespace is in hostname and must be signed
            if (s3Config.isUseVHost() && s3Config.isSignNamespace()) {
                String namespace = (String) request.getProperties().get(RestUtil.PROPERTY_NAMESPACE);
                if (namespace != null) resource = "/" + namespace + resource;
            }

            S3AuthUtil.sign(request.getMethod(),
                    resource,
                    parameters,
                    request.getHeaders(),
                    s3Config.getIdentity(),
                    s3Config.getSecretKey(),
                    s3Config.getServerClockSkew());
        }

        return getNext().handle(request);
    }
}
