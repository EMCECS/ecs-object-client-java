/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
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
