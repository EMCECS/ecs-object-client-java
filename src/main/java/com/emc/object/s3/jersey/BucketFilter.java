/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Config;
import com.emc.object.s3.S3Constants;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

public class BucketFilter extends ClientFilter {
    private static final Logger l4j = Logger.getLogger(BucketFilter.class);

    public static URI insertBucket(URI uri, String bucketName, boolean useVHost) {
        try {
            if (useVHost) { // prepend to hostname (i.e. bucket.s3.company.com)
                String hostname = bucketName + "." + uri.getHost();
                uri = new URI(uri.getScheme(), uri.getUserInfo(), hostname, uri.getPort(),
                        uri.getPath(), uri.getQuery(), uri.getFragment());

            } else { // prepend to resource path (i.e. s3.company.com/bucket)
                String resource = "/" + bucketName + uri.getPath();
                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                        resource, uri.getQuery(), uri.getFragment());
            }

            l4j.debug("URI including bucket: " + uri);
            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("bucket name \"%s\" generated an invalid URI", bucketName), e);
        }
    }

    private S3Config s3Config;

    public BucketFilter(S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        URI uri = request.getURI();

        String bucketName = (String) request.getProperties().get(S3Constants.PROPERTY_BUCKET_NAME);
        if (bucketName != null) {
            request.setURI(insertBucket(uri, bucketName, s3Config.isUseVHost()));
        }

        return getNext().handle(request);
    }
}
