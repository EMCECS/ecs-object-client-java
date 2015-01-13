package com.emc.object.s3;

import com.emc.object.ObjectConfig;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class S3Config extends ObjectConfig {
    private boolean vHostBuckets = false;
    private boolean vHostNamespace = false;
    private boolean signNamespace = false;

    /**
     * By default, the smart client is enabled, which means virtual host-style buckets/namespaces cannot be used. The
     * easiest way to enable virtual host-style anything is to call this method. This will disable the smart
     * client and set a single host endpoint as well as configure whether to include the namespace in the host name
     * and if so, whether it should be signed (should be true if your cloud has global users).
     */
    public void enableVirtualHosting(URI endpoint, boolean vHostNamespace, boolean signNamespace) {
        vHostBuckets = true;
        this.vHostNamespace = vHostNamespace;
        this.signNamespace = signNamespace;

        List<URI> endpoints = new ArrayList<>();
        endpoints.add(endpoint);
        setEndpoints(endpoints);

        // make sure we don't poll for individual nodes
        getProperties().put(ObjectConfig.PROPERTY_DISABLE_POLLING, "true");
    }

    @Override
    public String resolveHost() {
        return getEndpoints().get(0).getHost();
    }

    public boolean isvHostBuckets() {
        return vHostBuckets;
    }

    public void setvHostBuckets(boolean vHostBuckets) {
        this.vHostBuckets = vHostBuckets;
    }

    public boolean isvHostNamespace() {
        return vHostNamespace;
    }

    public void setvHostNamespace(boolean vHostNamespace) {
        this.vHostNamespace = vHostNamespace;
    }

    public boolean isSignNamespace() {
        return signNamespace;
    }

    public void setSignNamespace(boolean signNamespace) {
        this.signNamespace = signNamespace;
    }
}
