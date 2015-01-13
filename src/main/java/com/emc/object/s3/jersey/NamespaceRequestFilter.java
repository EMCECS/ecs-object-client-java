package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Constants;
import org.apache.log4j.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class NamespaceRequestFilter implements ClientRequestFilter {
    private static final Logger l4j = Logger.getLogger(NamespaceRequestFilter.class);

    public static final String PROPERTY_NAMESPACE = "com.emc.object.s3.namespace";

    private S3Config s3Config;

    public NamespaceRequestFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String namespace = (String) requestContext.getProperty(PROPERTY_NAMESPACE);
        if (namespace != null) {

            if (s3Config.isvHostNamespace()) { // prepend to hostname (i.e. namespace.s3.company.com)
                try {
                    URI uri = requestContext.getUri();
                    String hostname = namespace + "." + uri.getHost();
                    l4j.debug(String.format("hostname including namespace: %s", hostname));
                    requestContext.setUri(new URI(uri.getScheme(), uri.getUserInfo(), hostname, uri.getPort(),
                            uri.getPath(), uri.getQuery(), uri.getFragment()));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(String.format("namespace \"%s\" generated an invalid URI", namespace), e);
                }

            } else { // add to headers (x-emc-namespace: namespace)
                requestContext.getHeaders().putSingle(S3Constants.HEADER_NAMESPACE, namespace);
            }
        }
    }
}
