/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;
import com.emc.rest.smart.ecs.Vdc;

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
        super(Protocol.valueOf(endpoint.getScheme().toUpperCase()), endpoint.getPort(), new Vdc(endpoint.getHost()));

        // standard VHost type signs namespace
        useVHost = true;
        signNamespace = true;

        // make sure we disable "smart" features
        setProperty(ObjectConfig.PROPERTY_DISABLE_HOST_UPDATE, "true");
        setProperty(ObjectConfig.PROPERTY_DISABLE_HEALTH_CHECK, "true");
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
