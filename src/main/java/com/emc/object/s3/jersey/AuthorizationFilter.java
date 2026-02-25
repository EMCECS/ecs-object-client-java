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

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3Signer;
import com.emc.object.s3.VHostUtil;
import com.emc.object.util.RestUtil;

public class AuthorizationFilter implements ClientRequestFilter {
    private S3Config s3Config;
    private S3Signer signer;

    public AuthorizationFilter(S3Config s3Config, S3Signer signer) {
        this.s3Config = s3Config;
        this.signer = signer;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        // tack on user-agent here
        if (s3Config.getUserAgent() != null) {
            requestContext.getHeaders().putSingle(RestUtil.HEADER_USER_AGENT, s3Config.getUserAgent());
        }
        // if no identity is provided, this is an anonymous client
        if (s3Config.getIdentity() != null) {
            Map<String, String> parameters = RestUtil.getQueryParameterMap(requestContext.getUri().getRawQuery());

            String resource = VHostUtil.getResourceString(s3Config,
                    (String) requestContext.getProperty(RestUtil.PROPERTY_NAMESPACE),
                    (String) requestContext.getProperty(S3Constants.PROPERTY_BUCKET_NAME),
                    RestUtil.getEncodedPath(requestContext.getUri()));

            signer.sign(requestContext,
                    resource,
                    parameters,
                    requestContext.getHeaders());
        }
    }
}
