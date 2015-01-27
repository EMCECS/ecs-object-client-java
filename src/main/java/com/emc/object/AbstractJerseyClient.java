package com.emc.object;

import com.emc.object.util.RestUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

public abstract class AbstractJerseyClient {
    protected ObjectConfig objectConfig;

    protected AbstractJerseyClient(ObjectConfig objectConfig) {
        this.objectConfig = objectConfig;
    }

    protected Response executeRequest(Client client, ObjectRequest request) {
        Invocation.Builder builder = buildRequest(client, request);

        if (request.getMethod().isRequiresEntity()) {
            Entity entity = Entity.text("");
            if (request instanceof EntityRequest) {
                EntityRequest entityRequest = (EntityRequest) request;
                entity = Entity.entity(entityRequest.getEntity(), entityRequest.getContentType());
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
        builder.headers(new MultivaluedHashMap<String, Object>(request.getHeaders()));

        return builder;
    }
}
