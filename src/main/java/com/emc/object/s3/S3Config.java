/*
 * Copyright (c) 2015-2016, EMC Corporation.
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
import com.emc.object.util.ConfigUriProperty;
import com.emc.rest.smart.Host;
import com.emc.rest.smart.SmartConfig;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.rest.smart.jersey.SmartClientFactory;

import java.net.URI;

/**
 * To enable the smart-client with a single VDC, use the {@link #S3Config(Protocol, String...)} constructor:
 * <pre>
 *     S3Config s3Config = new S3Config(Protocol.HTTP, "10.10.10.11", "10.10.10.12");
 * </pre>
 * <p>
 * To enable the smart-client with multiple VDCs, use the {@link #S3Config(Protocol, Vdc...)} constructor:
 * <pre>
 *     S3Config s3Config = new S3Config(Protocol.HTTP, new Vdc("10.10.10.11", "10.10.10.12"), new Vdc("10.20.10.11", "10.20.10.12"));
 * </pre>
 * <p>
 * To use an external load balancer without virtual-host-style requests use the {@link #S3Config(URI)} constructor:
 * <pre>
 *     S3Config s3Config = new S3Config("https://10.10.10.10:8443");
 * </pre>
 * <p>
 * To use an external load balancer <em>with</em> virtual-host-style requests (where <code>bucket.namespace.</code> is
 * prepended to the hostname), use the {@link #S3Config(URI)} constructor and {@link #setUseVHost(boolean)} to true:
 * <pre>
 *     S3Config s3Config = new S3Config("https://s3.company.com").withUseVHost(true);
 * </pre>
 * <p>
 * <em>NOTE:</em> If you enable virtual-host-style requests, you must specify your namespace or set it to null and
 * include it in the hostname of the endpoint.
 */
public class S3Config extends ObjectConfig<S3Config> {
    public static final int DEFAULT_HTTP_PORT = 9020;
    public static final int DEFAULT_HTTPS_PORT = 9021;

    protected static int defaultPort(Protocol protocol) {
        if (protocol == Protocol.HTTP) return DEFAULT_HTTP_PORT;
        else if (protocol == Protocol.HTTPS) return DEFAULT_HTTPS_PORT;
        throw new IllegalArgumentException("unknown protocol: " + protocol);
    }

    // NOTE: if you add a property, make sure you add it to the cloning constructor!
    protected boolean useVHost = false;
    protected boolean signNamespace = true;
    protected boolean checksumEnabled = true;
    protected float faultInjectionRate = 0.0f;
    protected boolean signMetadataSearch = true;
    protected boolean useV2Signer = true;

    /**
     * Empty constructor for internal use only!
     */
    public S3Config() {
    }

    /**
     * External load balancer constructor (no smart-client).
     * <p>
     * <em>NOTE:</em> To use virtual-host-style requests where
     * <code>bucket.namespace.</code> is prepended to the host, you must {@link #setUseVHost(boolean)} to true.
     */
    public S3Config(URI endpoint) {
        super(endpoint);
    }

    /**
     * Single VDC smart-client constructor.
     */
    public S3Config(Protocol protocol, String... hostList) {
        super(protocol, defaultPort(protocol), hostList);
    }

    /**
     * Multiple VDC smart-client constructor.
     */
    public S3Config(Protocol protocol, Vdc... vdcs) {
        super(protocol, defaultPort(protocol), vdcs);
    }

    /**
     * Cloning constructor.
     */
    public S3Config(S3Config other) {
        super(other);
        this.useVHost = other.useVHost;
        this.signNamespace = other.signNamespace;
        this.checksumEnabled = other.checksumEnabled;
        this.faultInjectionRate = other.faultInjectionRate;
        this.signMetadataSearch = other.signMetadataSearch;
        this.useV2Signer = other.useV2Signer;
    }

    @Override
    public Host resolveHost() {
        return getVdcs().get(0).getHosts().get(0);
    }

    @Override
    public SmartConfig toSmartConfig() {
        // disable Apache client's retry - retries will be governed by our config and RetryFilter
        return super.toSmartConfig().withProperty(SmartClientFactory.DISABLE_APACHE_RETRY, Boolean.TRUE);
    }

    @ConfigUriProperty
    public boolean isUseVHost() {
        return useVHost;
    }

    /**
     * Set to true to enable virtual-host-style requests. This prepends namespaces and buckets as appropriate
     * for each request.
     * <p>
     * <em>NOTE:</em> To use virtual host configuration, you must disable the smart client by using the
     * {@link #S3Config(URI)} constructor. You must also specify your namespace or set it to null and include
     * it in the hostname of the endpoint.
     */
    public void setUseVHost(boolean useVHost) {
        this.useVHost = useVHost;
    }

    @ConfigUriProperty
    public boolean isSignNamespace() {
        return signNamespace;
    }

    /**
     * Standard ECS configurations require signing the namespace to support cross-namespace
     * requests. To change this behavior to a legacy type virtual host,
     * which is isolated to the default namespace of the user, set this to false.
     */
    public void setSignNamespace(boolean signNamespace) {
        this.signNamespace = signNamespace;
    }

    @ConfigUriProperty
    public boolean isChecksumEnabled() {
        return checksumEnabled;
    }

    /**
     * By default, MD5 sums are verified on whole-object reads and writes whenever possible.  You can disable that by
     * setting this to false.
     */
    public void setChecksumEnabled(boolean checksumEnabled) {
        this.checksumEnabled = checksumEnabled;
    }

    @ConfigUriProperty
    public float getFaultInjectionRate() {
        return faultInjectionRate;
    }

    /**
     * Sets the fault injection rate. Enables fault injection when this number is &gt; 0. The rate is a ratio expressed as
     * a decimal between 0 and 1.  This is the rate at which faults (HTTP 500 errors) should randomly be injected into
     * the response. When faults are injected, the real request is never sent over the wire. Fault injection is disabled
     * by default.
     */
    public void setFaultInjectionRate(float faultInjectionRate) {
        this.faultInjectionRate = faultInjectionRate;
    }

    @ConfigUriProperty
    public boolean isSignMetadataSearch() {
        return signMetadataSearch;
    }

    /**
     * Instructs the client whether to sign the metadata search parameters. Prior to ECS 3.0, these were not signed,
     * but as of 3.0 they are now signed. Be sure to set this appropriately based on your ECS software version.
     * <p>Note: as of object-client 3.0, this defaults to true (MD search params will be signed)</p>
     */
    public void setSignMetadataSearch(boolean signMetadataSearch) {
        this.signMetadataSearch = signMetadataSearch;
    }

    @ConfigUriProperty
    public boolean isUseV2Signer() {
        return useV2Signer;
    }

    /**
     * Sets the AWS signature version for authentication requests, default is v2,
     * set this to false to use v4.
     */
    public void setUseV2Signer(boolean useV2Signer) {
        this.useV2Signer = useV2Signer;
    }

    public S3Config withUseVHost(boolean useVHost) {
        setUseVHost(useVHost);
        return this;
    }

    public S3Config withSignNamespace(boolean signNamespace) {
        setSignNamespace(signNamespace);
        return this;
    }

    public S3Config withChecksumEnabled(boolean checksumEnabled) {
        setChecksumEnabled(checksumEnabled);
        return this;
    }

    public S3Config withFaultInjectionRate(float faultInjectionRate) {
        setFaultInjectionRate(faultInjectionRate);
        return this;
    }

    public S3Config withSignMetadataSearch(boolean signMetadataSearch) {
        setSignMetadataSearch(signMetadataSearch);
        return this;
    }

    public S3Config withUseV2Signer(boolean useV2Signer) {
        setUseV2Signer(useV2Signer);
        return this;
    }

    @Override
    public String toString() {
        return "S3Config{" +
                "useVHost=" + useVHost +
                ", signNamespace=" + signNamespace +
                ", checksumEnabled=" + checksumEnabled +
                ", retryEnabled=" + retryEnabled +
                ", initialRetryDelay=" + initialRetryDelay +
                ", retryLimit=" + retryLimit +
                ", retryBufferSize=" + retryBufferSize +
                ", faultInjectionRate=" + faultInjectionRate +
                ", signMetadataSearch=" + signMetadataSearch +
                ", useV2Signer=" + useV2Signer +
                "} " + super.toString();
    }
}
