/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Config;
import com.emc.object.util.RestUtil;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

public class NamespaceFilter extends ClientFilter {
    private static final Logger l4j = Logger.getLogger(NamespaceFilter.class);

    /**
     * prepend to hostname (i.e. namespace.s3.company.com)
     */
    public static URI insertNamespace(URI uri, String namespace) {
        try {
            String hostname = namespace + "." + uri.getHost();
            l4j.debug(String.format("hostname including namespace: %s", hostname));
            return new URI(uri.getScheme(), uri.getUserInfo(), hostname, uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("namespace \"%s\" generated an invalid URI", namespace), e);
        }
    }

    private S3Config s3Config;

    public NamespaceFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        String namespace = (String) request.getProperties().get(RestUtil.PROPERTY_NAMESPACE);
        if (namespace != null) {

            if (s3Config.isUseVHost()) {
                request.setURI(insertNamespace(request.getURI(), namespace));
            } else {
                // add to headers (x-emc-namespace: namespace)
                request.getHeaders().putSingle(RestUtil.EMC_NAMESPACE, namespace);
            }
        }

        return getNext().handle(request);
    }
}
