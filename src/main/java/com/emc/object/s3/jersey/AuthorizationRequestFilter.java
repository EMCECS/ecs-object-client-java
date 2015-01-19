package com.emc.object.s3.jersey;

import com.emc.object.s3.S3AuthUtil;
import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Constants;
import com.emc.object.util.RestUtil;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.Map;

public class AuthorizationRequestFilter implements ClientRequestFilter {
    private S3Config s3Config;

    public AuthorizationRequestFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        // if no identity is provided, this is an anonymous client
        if (s3Config.getIdentity() != null) {
            Map<String, String> parameters = RestUtil.getQueryParameterMap(requestContext.getUri().getQuery());
            String resource = requestContext.getUri().getPath();

            // check if bucket is in hostname
            if (s3Config.isvHostBuckets()) {
                String bucketName = (String) requestContext.getProperty(S3Constants.PROPERTY_BUCKET_NAME);
                if (bucketName != null) resource = "/" + bucketName + resource;
            }

            // check if namespace is in hostname
            if (s3Config.isvHostNamespace() && s3Config.isSignNamespace()) {
                String namespace = (String) requestContext.getProperty(RestUtil.PROPERTY_NAMESPACE);
                if (namespace != null) resource = "/" + namespace + resource;
            }

            S3AuthUtil.sign(requestContext.getMethod(),
                    resource,
                    parameters,
                    requestContext.getHeaders(),
                    s3Config.getIdentity(),
                    s3Config.getSecretKey(),
                    s3Config.getServerClockSkew());
        }
    }
}
