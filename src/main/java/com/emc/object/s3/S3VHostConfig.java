/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;

import java.net.URI;

/**
 * This configuration uses a single named host as the endpoint and prepends namespaces and buckets as appropriate
 * for each request. Standard virtual host configurations require signing the namespace to support cross-namespace
 * requests. This is the default behavior of this configuration. To change this behavior to a legacy type virtual host
 * which is isolated to the default namespace of the user, call {@link #setLegacyMode(boolean)} and pass
 * <code>true</code> as the argument.
 * <p/>
 * <em>NOTE:</em> To use virtual host configuration, you must specify your namespace or set it to null and include it
 * in the hostname of the endpoint.
 */
public class S3VHostConfig extends S3Config {
    public S3VHostConfig(URI endpoint) {
        super(Protocol.valueOf(endpoint.getScheme().toUpperCase()), endpoint.getPort(), endpoint.getHost());

        // standard VHost type signs namespace
        useVHost = true;
        signNamespace = true;

        // make sure we don't poll for individual nodes
        property(ObjectConfig.PROPERTY_DISABLE_POLLING, "true");
    }

    /**
     * Set to true to enable legacy S3 behavior where each request is isolated to the default namespace of the user.
     */
    public void setLegacyMode(boolean legacyMode) {
        signNamespace = !legacyMode;
    }

    public boolean isLegacyMode() {
        return !signNamespace;
    }
}
