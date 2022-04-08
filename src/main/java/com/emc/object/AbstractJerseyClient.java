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
package com.emc.object;

import com.emc.object.util.RestUtil;
import com.emc.rest.smart.jersey.SizeOverrideWriter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJerseyClient {

    private static final Logger log = LoggerFactory.getLogger(AbstractJerseyClient.class);

    protected ObjectConfig objectConfig;

    protected AbstractJerseyClient(ObjectConfig objectConfig) {
        this.objectConfig = objectConfig;
    }

    protected ClientResponse executeAndClose(Client client, ObjectRequest request) {
        ClientResponse response = executeRequest(client, request);
        response.close();
        return response;
    }

    @SuppressWarnings("unchecked")
    protected ClientResponse executeRequest(Client client, ObjectRequest request) {
        try {
            if (request.getMethod().isRequiresEntity()) {
                String contentType = RestUtil.DEFAULT_CONTENT_TYPE;
                Object entity = new byte[0];
                if (request instanceof EntityRequest) {
                    EntityRequest entityRequest = (EntityRequest) request;

                    if (entityRequest.getContentType() != null) contentType = entityRequest.getContentType();

                    if (entityRequest.getEntity() != null) entity = entityRequest.getEntity();

                    // if content-length is set (perhaps by user), force jersey to use it
                    if (entityRequest.getContentLength() != null) {
                        log.debug("enabling content-length override ({})", entityRequest.getContentLength().toString());
                        SizeOverrideWriter.setEntitySize(entityRequest.getContentLength());

                        // otherwise chunked encoding will be used. if the request does not support it, try to ensure
                        // that the entity is buffered (will set content length from buffered write)
                    } else if (!entityRequest.isChunkable()) {
                        log.debug("no content-length and request is not chunkable, attempting to enable buffering");
                        request.property(ApacheHttpClient4Config.PROPERTY_ENABLE_BUFFERING, Boolean.TRUE);
                        request.property(ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, null);
                    }
                } else {

                    // no entity, but make sure the apache handler doesn't mess up the content-length somehow
                    // (i.e. if content-encoding is set)
                    request.property(ApacheHttpClient4Config.PROPERTY_ENABLE_BUFFERING, Boolean.TRUE);

                    String headerContentType = RestUtil.getFirstAsString(request.getHeaders(), RestUtil.HEADER_CONTENT_TYPE);
                    if (headerContentType != null) contentType = headerContentType;
                }

                WebResource.Builder builder = buildRequest(client, request);

                // jersey requires content-type for entity requests
                builder.type(contentType);
                return builder.method(request.getMethod().toString(), ClientResponse.class, entity);
            } else { // non-entity request method

                // can't send content with non-entity methods (GET, HEAD, etc.)
                if (request instanceof EntityRequest)
                    throw new UnsupportedOperationException("an entity request is using a non-entity method (" + request.getMethod() + ")");

                WebResource.Builder builder = buildRequest(client, request);

                return builder.method(request.getMethod().toString(), ClientResponse.class);
            }
        } finally {
            // make sure we clear the content-length override for this thread
            SizeOverrideWriter.setEntitySize(null);
        }
    }

    protected <T> T executeRequest(Client client, ObjectRequest request, Class<T> responseType) {
        ClientResponse response = executeRequest(client, request);
        T responseEntity = response.getEntity(responseType);
        fillResponseEntity(responseEntity, response);
        return responseEntity;
    }

    protected void fillResponseEntity(Object responseEntity, ClientResponse response) {
        if (responseEntity instanceof ObjectResponse)
            ((ObjectResponse) responseEntity).setHeaders(response.getHeaders());
    }

    protected WebResource.Builder buildRequest(Client client, ObjectRequest request) {
        URI uri = objectConfig.resolvePath(request.getPath(), request.getRawQueryString());
        WebResource resource = client.resource(uri);

        // set properties
        for (Map.Entry<String, Object> entry : request.getProperties().entrySet()) {
            resource.setProperty(entry.getKey(), entry.getValue());
        }

        // set namespace
        String namespace = request.getNamespace() != null ? request.getNamespace() : objectConfig.getNamespace();
        if (namespace != null)
            resource.setProperty(RestUtil.PROPERTY_NAMESPACE, namespace);

        WebResource.Builder builder = resource.getRequestBuilder();

        // set headers
        for (String name : request.getHeaders().keySet()) {
            for (Object value : request.getHeaders().get(name)) {
                builder = builder.header(name, value);
            }
        }

        return builder;
    }

    public ObjectConfig getObjectConfig() {
        return objectConfig;
    }
}
