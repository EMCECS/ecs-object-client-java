package com.emc.object;

import com.emc.object.util.RestUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MultivaluedHashMap;

public abstract class AbstractJerseyClient {
    protected ObjectConfig objectConfig;

    protected AbstractJerseyClient(ObjectConfig objectConfig) {
        this.objectConfig = objectConfig;
    }

    protected <T> T executeRequest(Client client, ObjectRequest request, Class<T> responseType) {
        Invocation.Builder builder = buildRequest(client, request);

        if (request instanceof EntityRequest && ((EntityRequest) request).getEntity() != null) {
            EntityRequest entityRequest = (EntityRequest) request;
            Entity entity = Entity.entity(entityRequest.getEntity(), entityRequest.getContentType());

            if (responseType != null) {
                return builder.method(request.getMethod().toString(), entity, responseType);
            } else {
                builder.method(request.getMethod().toString(), entity);
                return null;
            }

        } else {
            if (responseType != null) {
                return builder.method(request.getMethod().toString(), responseType);
            } else {
                builder.method(request.getMethod().toString());
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
