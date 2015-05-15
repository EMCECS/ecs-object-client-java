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

import com.emc.object.s3.request.EncodingType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ListBucketResult")
public class ListObjectsResult {
    private String bucketName;
    private String prefix;
    private String delimiter;
    private Integer maxKeys;
    private EncodingType encodingType;
    private String marker;
    private String nextMarker;
    private Boolean truncated;
    private List<S3Object> objects = new ArrayList<S3Object>();
    private List<CommonPrefix> _commonPrefixes = new ArrayList<CommonPrefix>();

    @XmlElement(name = "Name")
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @XmlElement(name = "Prefix")
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @XmlElement(name = "Delimiter")
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @XmlElement(name = "MaxKeys")
    public Integer getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(Integer maxKeys) {
        this.maxKeys = maxKeys;
    }

    @XmlElement(name = "Encoding-Type")
    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
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

    @XmlElement(name = "IsTruncated")
    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    @XmlElement(name = "Contents")
    public List<S3Object> getObjects() {
        return objects;
    }

    public void setObjects(List<S3Object> objects) {
        this.objects = objects;
    }

    @XmlElement(name = "CommonPrefixes")
    protected List<CommonPrefix> get_commonPrefixes() {
        return _commonPrefixes;
    }

    protected void set_commonPrefixes(List<CommonPrefix> _commonPrefixes) {
        this._commonPrefixes = _commonPrefixes;
    }

    @XmlTransient
    public List<String> getCommonPrefixes() {
        List<String> prefixes = new ArrayList<String>();
        for (CommonPrefix prefix : _commonPrefixes) {
            prefixes.add(prefix.getPrefix());
        }
        return prefixes;
    }
}
