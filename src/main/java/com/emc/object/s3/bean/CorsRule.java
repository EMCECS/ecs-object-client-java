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
package com.emc.object.s3.bean;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlType(propOrder = {"id", "allowedOrigins", "allowedMethods", "maxAgeSeconds", "allowedHeaders", "exposeHeaders"})
public class CorsRule {
    private String id;
    private List<CorsMethod> allowedMethods = new ArrayList<CorsMethod>();
    private List<String> allowedOrigins = new ArrayList<String>();
    private Integer maxAgeSeconds;
    private List<String> exposeHeaders = new ArrayList<String>();
    private List<String> allowedHeaders = new ArrayList<String>();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CorsRule)) return false;

        CorsRule corsRule = (CorsRule) o;

        if (id != null ? !id.equals(corsRule.id) : corsRule.id != null) return false;
        if (allowedMethods != null ? !allowedMethods.equals(corsRule.allowedMethods) : corsRule.allowedMethods != null)
            return false;
        if (allowedOrigins != null ? !allowedOrigins.equals(corsRule.allowedOrigins) : corsRule.allowedOrigins != null)
            return false;
        if (maxAgeSeconds != null ? !maxAgeSeconds.equals(corsRule.maxAgeSeconds) : corsRule.maxAgeSeconds != null)
            return false;
        if (exposeHeaders != null ? !exposeHeaders.equals(corsRule.exposeHeaders) : corsRule.exposeHeaders != null)
            return false;
        return !(allowedHeaders != null ? !allowedHeaders.equals(corsRule.allowedHeaders) : corsRule.allowedHeaders != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (allowedMethods != null ? allowedMethods.hashCode() : 0);
        result = 31 * result + (allowedOrigins != null ? allowedOrigins.hashCode() : 0);
        result = 31 * result + (maxAgeSeconds != null ? maxAgeSeconds.hashCode() : 0);
        result = 31 * result + (exposeHeaders != null ? exposeHeaders.hashCode() : 0);
        result = 31 * result + (allowedHeaders != null ? allowedHeaders.hashCode() : 0);
        return result;
    }
}
