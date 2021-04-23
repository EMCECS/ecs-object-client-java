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

import com.emc.object.util.RestUtil;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ListBucketResult")
public class ListObjectsResult implements UrlEncodable {
    private String bucketName;
    private String prefix;
    private String delimiter;
    private Integer maxKeys;
    private EncodingType encodingType;
    private String marker;
    private String nextMarker;
    private boolean truncated;
    private boolean encoded = false;
    private List<S3Object> objects = new ArrayList<S3Object>();
    private List<CommonPrefix> _commonPrefixes = new ArrayList<CommonPrefix>();

    //This method is called after all the properties (except IDREF) are unmarshalled for this object,
    //but before this object is set to the parent object.
    void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if (encodingType == EncodingType.url) {
            // url-decode applicable values (bucketName, prefix, delimiter, marker, nextMarker)
            bucketName = RestUtil.urlDecode(bucketName, false);
            prefix = RestUtil.urlDecode(prefix, false);
            delimiter = RestUtil.urlDecode(delimiter, false);
            marker = RestUtil.urlDecode(marker, false);
            nextMarker = RestUtil.urlDecode(nextMarker, false);
        }
    }

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

    @XmlElement(name = "EncodingType")
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
    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    @XmlElement(name = "Contents")
    public List<S3Object> getObjects() {
        if (encodingType == EncodingType.url && !encoded) {
            List<S3Object> encodedObjects = new ArrayList<S3Object>();
            if(objects != null && objects.size() != 0) {
                for(S3Object object: objects) {
                    S3Object encodedObject = object;
                    encodedObject.setKey(RestUtil.urlEncode(object.getKey()));
                    encodedObjects.add(encodedObject);
                }
                encoded = true;
                return encodedObjects;
            }
        }
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
