package com.emc.object.util;

import com.emc.rest.smart.HostListProvider;
import com.emc.rest.smart.LoadBalancer;
import com.emc.rest.smart.bean.ListDataNode;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
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
        String signature = null;
        try {
            signature = getSignature(canonicalString, secret);
        } catch (Exception e) {
            throw new RuntimeException("could not generate signature", e);
        }

        // construct request
        Invocation.Builder request = client.target(uri).request();

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
