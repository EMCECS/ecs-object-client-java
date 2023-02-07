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
import com.emc.object.util.RestUtil;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority(FilterPriorities.PRIORITY_NAMESPACE)
public class NamespaceFilter implements ClientRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(NamespaceFilter.class);

    /**
     * prepend to hostname (i.e. namespace.s3.company.com)
     */
    public static URI insertNamespace(URI uri, String namespace) {
        try {
            String hostname = namespace + "." + uri.getHost();
            log.debug("hostname including namespace: {}", hostname);
            return RestUtil.replaceHost(uri, hostname);
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("namespace \"%s\" generated an invalid URI", namespace), e);
        }
    }

    private S3Config s3Config;

    public NamespaceFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public void filter(ClientRequestContext request) throws IOException {
        String namespace = (String) request.getConfiguration().getProperty(RestUtil.PROPERTY_NAMESPACE);

        if (namespace != null) {

            if (s3Config.isUseVHost()) {
                request.setUri(insertNamespace(request.getUri(), namespace));
            } else {
                // add to headers (x-emc-namespace: namespace)
                request.getHeaders().putSingle(RestUtil.EMC_NAMESPACE, namespace);
            }
        }
    }
}
