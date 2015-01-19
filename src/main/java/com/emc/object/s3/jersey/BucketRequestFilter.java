package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Constants;
import org.apache.log4j.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class BucketRequestFilter implements ClientRequestFilter {
    private static final Logger l4j = Logger.getLogger(BucketRequestFilter.class);

    private S3Config s3Config;

    public BucketRequestFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        URI uri = requestContext.getUri();

        String bucketName = (String) requestContext.getProperty(S3Constants.PROPERTY_BUCKET_NAME);
        try {
            if (bucketName != null) {

                if (s3Config.isvHostBuckets()) { // prepend to hostname (i.e. bucket.s3.company.com)
                    String hostname = bucketName + "." + uri.getHost();
                    uri = new URI(uri.getScheme(), uri.getUserInfo(), hostname, uri.getPort(),
                            uri.getPath(), uri.getQuery(), uri.getFragment());

                } else { // prepend to resource path (i.e. s3.company.com/bucket)
                    String resource = "/" + bucketName + uri.getPath();
                    uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                            resource, uri.getQuery(), uri.getFragment());
                }

                l4j.debug("URI including bucket: " + uri);

                requestContext.setUri(uri);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("bucket name \"%s\" generated an invalid URI", bucketName), e);
        }
    }
}
