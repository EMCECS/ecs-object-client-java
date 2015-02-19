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

import com.emc.object.s3.bean.ListDataNode;
import com.emc.rest.smart.HostListProvider;
import com.emc.rest.smart.LoadBalancer;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;

public class S3HostListProvider implements HostListProvider {
    private static final Logger l4j = Logger.getLogger(S3HostListProvider.class);

    public static final String DEFAULT_PROTOCOL = "https";
    public static final int DEFAULT_PORT = 9021;

    protected static final SimpleDateFormat rfc822DateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        rfc822DateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
    }

    private Client client;
    private LoadBalancer loadBalancer;
    private String user;
    private String secret;
    private String protocol = DEFAULT_PROTOCOL;
    private int port = DEFAULT_PORT;

    public S3HostListProvider(Client client, LoadBalancer loadBalancer, String user, String secret) {
        this.client = client;
        this.loadBalancer = loadBalancer;
        this.user = user;
        this.secret = secret;
    }

    public List<String> getHostList() {
        String host = loadBalancer.getTopHost().getName();
        String portStr = (port > -1) ? ":" + port : "";
        String path = "/?endpoint";
        String uri = protocol + "://" + host + portStr + path;

        // format date
        String rfcDate;
        synchronized (rfc822DateFormat) {
            rfcDate = rfc822DateFormat.format(new Date());
        }

        // generate signature
        String canonicalString = "GET\n\n\n" + rfcDate + "\n" + path;
        String signature;
        try {
            signature = getSignature(canonicalString, secret);
        } catch (Exception e) {
            throw new RuntimeException("could not generate signature", e);
        }

        // construct request
        WebResource.Builder request = client.resource(uri).getRequestBuilder();

        // add date and auth headers
        request.header("Date", rfcDate);
        request.header("Authorization", "AWS " + user + ":" + signature);

        // make REST call
        return request.get(ListDataNode.class).getDataNodes();
    }

    protected String getSignature(String canonicalString, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA1"));
        String signature = new String(Base64.encodeBase64(mac.doFinal(canonicalString.getBytes("UTF-8"))));
        l4j.debug("canonicalString:\n" + canonicalString);
        l4j.debug("signature:\n" + signature);
        return signature;
    }

    public Client getClient() {
        return client;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public String getUser() {
        return user;
    }

    public String getSecret() {
        return secret;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
