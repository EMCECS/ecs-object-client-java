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

    protected <T> T executeRequest(Client client, ObjectRequest request, Class<T> responseType) {
        Invocation.Builder builder = buildRequest(client, request);

        if (request.getMethod().isRequiresEntity()) {
            Entity entity = Entity.text("");
            if (request instanceof EntityRequest) {
                EntityRequest entityRequest = (EntityRequest) request;
                entity = Entity.entity(entityRequest.getEntity(), entityRequest.getContentType());
            }

            if (responseType != null) {
                return builder.method(request.getMethod().toString(), entity, responseType);
            } else {
                Response response = builder.method(request.getMethod().toString(), entity);
                response.close();
                return null;
            }
        } else { // non-entity request method

            // can't send content with non-entity methods (GET, HEAD, etc.)
            if (request instanceof EntityRequest)
                throw new UnsupportedOperationException("an entity request is using a non-entity method (" + request.getMethod() + ")");

            if (responseType != null) {
                return builder.method(request.getMethod().toString(), responseType);
            } else {
                Response response = builder.method(request.getMethod().toString());
                response.close();
                return null;
            }
        }
    }

    protected Invocation.Builder buildRequest(Client client, ObjectRequest request) {
        Invocation.Builder builder = client.target(objectConfig.resolvePath(request.getPath(), request.getQuery())).request();

        // set namespace
        String namespace = request.getNamespace() != null ? request.getNamespace() : objectConfig.getNamespace();
        if (namespace != null)
            builder.property(RestUtil.PROPERTY_NAMESPACE, namespace);

        // set headers
        builder.headers(new MultivaluedHashMap<String, Object>(request.getHeaders()));

        return builder;
    }
}
