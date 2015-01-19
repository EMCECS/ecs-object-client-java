package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlType(propOrder = {"id", "allowedOrigins", "allowedMethods", "maxAgeSeconds", "allowedHeaders", "exposeHeaders"})
public class CorsRule {
    private String id;
    private List<CorsMethod> allowedMethods = new ArrayList<>();
    private List<String> allowedOrigins = new ArrayList<>();
    private Integer maxAgeSeconds;
    private List<String> exposeHeaders = new ArrayList<>();
    private List<String> allowedHeaders = new ArrayList<>();

    @XmlElement(name = "ID")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "AllowedMethod")
    public List<CorsMethod> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<CorsMethod> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    @XmlElement(name = "AllowedOrigin")
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @XmlElement(name = "MaxAgeSeconds")
    public Integer getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public void setMaxAgeSeconds(Integer maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    @XmlElement(name = "ExposeHeader")
    public List<String> getExposeHeaders() {
        return exposeHeaders;
    }

    public void setExposeHeaders(List<String> exposeHeaders) {
        this.exposeHeaders = exposeHeaders;
    }

    @XmlElement(name = "AllowedHeader")
    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public CorsRule withId(String id) {
        setId(id);
        return this;
    }

    public CorsRule withAllowedMethods(List<CorsMethod> allowedMethods) {
        setAllowedMethods(allowedMethods);
        return this;
    }

    public CorsRule withAllowedMethods(CorsMethod... allowedMethods) {
        return withAllowedMethods(Arrays.asList(allowedMethods));
    }

    public CorsRule withAllowedOrigins(List<String> allowedOrigins) {
        setAllowedOrigins(allowedOrigins);
        return this;
    }

    public CorsRule withAllowedOrigins(String... allowedOrigins) {
        return withAllowedOrigins(Arrays.asList(allowedOrigins));
    }

    public CorsRule withMaxAgeSeconds(Integer maxAgeSeconds) {
        setMaxAgeSeconds(maxAgeSeconds);
        return this;
    }

    public CorsRule withExposeHeaders(List<String> exposeHeaders) {
        setExposeHeaders(exposeHeaders);
        return this;
    }

    public CorsRule withExposeHeaders(String... exposeHeaders) {
        return withExposeHeaders(Arrays.asList(exposeHeaders));
    }

    public CorsRule withAllowedHeaders(List<String> allowedHeaders) {
        setAllowedHeaders(allowedHeaders);
        return this;
    }

    public CorsRule withAllowedHeaders(String... allowedHeaders) {
        return withAllowedHeaders(Arrays.asList(allowedHeaders));
    }
}
