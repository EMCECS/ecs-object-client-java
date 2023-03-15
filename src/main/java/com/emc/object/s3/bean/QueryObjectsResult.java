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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "BucketQueryResult", namespace = "")
@XmlType(propOrder = {"bucketName", "marker", "nextMarker", "maxKeys", "objects", "prefixGroups"}, namespace = "")
public class QueryObjectsResult {
    private String bucketName;
    private Integer maxKeys;
    private String marker;
    private String nextMarker;
    private List<QueryObject> objects = new ArrayList<QueryObject>();
    private String query;
    private List<String> attributes = new ArrayList<String>();
    private String sorted;
    private boolean includeOlderVersions = false;
    private List<String> prefixGroups = new ArrayList<String>();

    @XmlElement(name = "Name")
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @XmlElement(name = "Marker")
    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    @XmlElement(name = "NextMarker")
    public String getNextMarker() {
        return nextMarker;
    }

    public void setNextMarker(String nextMarker) {
        this.nextMarker = nextMarker;
    }

    @XmlElement(name = "MaxKeys")
    public Integer getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(Integer maxKeys) {
        this.maxKeys = maxKeys;
    }

    @XmlTransient
    public boolean isTruncated() {
        return nextMarker != null && !nextMarker.isEmpty() && !"NO MORE PAGES".equals(nextMarker);
    }

    @XmlElementWrapper(name = "ObjectMatches")
    @XmlElement(name = "object")
    public List<QueryObject> getObjects() {
        return objects;
    }

    public void setObjects(List<QueryObject> objects) {
        this.objects = objects;
    }

    // context information for 'next' query
    @XmlTransient
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @XmlTransient
    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    @XmlTransient
    public String getSorted() {
        return sorted;
    }

    public void setSorted(String sorted) {
        this.sorted = sorted;
    }

    @XmlTransient
    public boolean getIncludeOlderVersions() {
        return includeOlderVersions;
    }

    public void setIncludeOlderVersions(boolean includeOlderVersions) {
        this.includeOlderVersions = includeOlderVersions;
    }

    @XmlElementWrapper(name = "CommonPrefixMatches")
    @XmlElement(name = "PrefixGroups")
    public List<String> getPrefixGroups() {
        return prefixGroups;
    }

    public void setPrefixGroups(List<String> prefixGroups) {
        this.prefixGroups = prefixGroups;
    }
}
