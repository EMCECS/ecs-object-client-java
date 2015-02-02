/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Config;
import com.emc.object.util.RestUtil;
import org.apache.log4j.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class NamespaceRequestFilter implements ClientRequestFilter {
    private static final Logger l4j = Logger.getLogger(NamespaceRequestFilter.class);

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

    public NamespaceRequestFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String namespace = (String) requestContext.getProperty(RestUtil.PROPERTY_NAMESPACE);
        if (namespace != null) {

            if (s3Config.isUseVHost()) {
                requestContext.setUri(insertNamespace(requestContext.getUri(), namespace));
            } else {
                // add to headers (x-emc-namespace: namespace)
                requestContext.getHeaders().putSingle(RestUtil.EMC_NAMESPACE, namespace);
            }
        }
    }
}
