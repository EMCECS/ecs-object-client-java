/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
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
            String resource = request.getURI().getPath();

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
