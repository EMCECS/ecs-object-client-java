/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;

/**
 * By default, the smart client is enabled, which means virtual host-style buckets/namespaces cannot be used. To use
 * virtual host-style requests, construct an {@link S3VHostConfig} instead. That will disable the smart
 * client and set a single host endpoint, prepending namespaces and buckets as appropriate.
 */
public class S3Config extends ObjectConfig<S3Config> {
    public static final int DEFAULT_HTTP_PORT = 9020;
    public static final int DEFAULT_HTTPS_PORT = 9021;

    protected static int defaultPort(Protocol protocol) {
        if (protocol == Protocol.HTTP) return DEFAULT_HTTP_PORT;
        else if (protocol == Protocol.HTTPS) return DEFAULT_HTTPS_PORT;
        throw new IllegalArgumentException("unknown protocol: " + protocol);
    }

    protected boolean useVHost = false;
    protected boolean signNamespace = true;

    public S3Config(Protocol protocol, String... hostList) {
        super(protocol, defaultPort(protocol), hostList);
    }

    public S3Config(Protocol protocol, int port, String... hostList) {
        super(protocol, port, hostList);
    }

    @Override
    public String resolveHost() {
        return getHosts().get(0);
    }

    public boolean isUseVHost() {
        return useVHost;
    }

    public boolean isSignNamespace() {
        return signNamespace;
    }
}
