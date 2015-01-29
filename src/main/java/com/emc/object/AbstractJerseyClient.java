package com.emc.object;

import com.emc.object.util.RestUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.InputStream;

public abstract class AbstractJerseyClient {
    protected ObjectConfig objectConfig;

    protected AbstractJerseyClient(ObjectConfig objectConfig) {
        this.objectConfig = objectConfig;
    }

    protected Response executeAndClose(Client client, ObjectRequest request) {
        Response response = executeRequest(client, request);
        response.close();
        return response;
    }

    protected Response executeRequest(Client client, ObjectRequest request) {
        Invocation.Builder builder = buildRequest(client, request);

        if (request.getMethod().isRequiresEntity()) {
            Entity entity = Entity.text("");
            if (request instanceof EntityRequest) {
                EntityRequest entityRequest = (EntityRequest) request;

                String contentType = entityRequest.getContentType();
                // jersey requires content-type for entity requests
                if (contentType == null) contentType = RestUtil.DEFAULT_CONTENT_TYPE;

                entity = Entity.entity(entityRequest.getEntity(), contentType);

                // make sure input streams have a content length
                if (entityRequest.getEntity() instanceof InputStream) {
                    if (entityRequest.getContentLength() == null)
                        throw new UnsupportedOperationException("you must specify a content length with an input stream");
                    builder.header(RestUtil.HEADER_CONTENT_LENGTH, entityRequest.getContentLength());
                }
            }

            return builder.method(request.getMethod().toString(), entity);
        } else { // non-entity request method

            // can't send content with non-entity methods (GET, HEAD, etc.)
            if (request instanceof EntityRequest)
                throw new UnsupportedOperationException("an entity request is using a non-entity method (" + request.getMethod() + ")");

            return builder.method(request.getMethod().toString());
        }
    }

    protected <T> T executeRequest(Client client, ObjectRequest request, Class<T> responseType) {
        Response response = executeRequest(client, request);
        T responseEntity = response.readEntity(responseType);
        fillResponseEntity(responseEntity, response);
        return responseEntity;
    }

    protected void fillResponseEntity(Object responseEntity, Response response) {
        if (responseEntity instanceof ObjectResponse)
            ((ObjectResponse) responseEntity).setHeaders(response.getHeaders());
    }

    protected Invocation.Builder buildRequest(Client client, ObjectRequest request) {
        Invocation.Builder builder = client.target(objectConfig.resolvePath(request.getPath(), request.getQueryString())).request();

        // set namespace
        String namespace = request.getNamespace() != null ? request.getNamespace() : objectConfig.getNamespace();
        if (namespace != null)
            builder.property(RestUtil.PROPERTY_NAMESPACE, namespace);

        // set headers
        MultivaluedMap<String, Object> jerseyHeaders = new MultivaluedHashMap<>();
        jerseyHeaders.putAll(request.getHeaders());
        builder.headers(jerseyHeaders);

        return builder;
    }
}
