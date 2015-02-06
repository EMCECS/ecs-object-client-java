/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object;

import com.emc.object.util.RestUtil;
import com.emc.rest.smart.SizeOverrideWriter;
import com.emc.rest.util.SizedInputStream;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public abstract class AbstractJerseyClient {
    protected ObjectConfig objectConfig;

    protected AbstractJerseyClient(ObjectConfig objectConfig) {
        this.objectConfig = objectConfig;
    }

    protected ClientResponse executeAndClose(Client client, ObjectRequest request) {
        ClientResponse response = executeRequest(client, request);
        response.close();
        return response;
    }

    protected ClientResponse executeRequest(Client client, ObjectRequest request) {
        WebResource.Builder builder = buildRequest(client, request);

        if (request.getMethod().isRequiresEntity()) {
            String contentType = RestUtil.DEFAULT_CONTENT_TYPE;
            Object entity = new byte[0];
            if (request instanceof EntityRequest) {
                EntityRequest entityRequest = (EntityRequest) request;

                if (entityRequest.getContentType() != null) contentType = entityRequest.getContentType();

                if (entityRequest.getEntity() != null) entity = entityRequest.getEntity();

                // override content-length if set
                if (entityRequest.getContentLength() != null) {
                    SizeOverrideWriter.setEntitySize(entityRequest.getContentLength());
                } else if (entity instanceof InputStream && !(entity instanceof SizedInputStream)) {
                    // TODO: can remove this when chunked encoding is supported
                    throw new UnsupportedOperationException("you must specify a content length with an input stream");
                }
            }

            // jersey requires content-type for entity requests
            builder.type(contentType);
            return builder.method(request.getMethod().toString(), ClientResponse.class, entity);
        } else { // non-entity request method

            // can't send content with non-entity methods (GET, HEAD, etc.)
            if (request instanceof EntityRequest)
                throw new UnsupportedOperationException("an entity request is using a non-entity method (" + request.getMethod() + ")");

            return builder.method(request.getMethod().toString(), ClientResponse.class);
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
        URI uri = objectConfig.resolvePath(request.getPath(), request.getQueryString());
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
}
