package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.Range;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.s3.bean.AccessControlList;
import com.emc.object.s3.bean.CannedAcl;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class PutObjectRequest<T> extends S3ObjectRequest implements EntityRequest<T> {
    private S3ObjectMetadata objectMetadata;
    private T object;
    private Range range;
    private AccessControlList acl;
    private CannedAcl cannedAcl;

    public PutObjectRequest(String bucketName, String key, T object) {
        super(Method.PUT, bucketName, key, null);
        this.object = object;
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        headers.putAll(objectMetadata.toHeaders());
        if (acl != null) headers.putAll(acl.toHeaders());
        if (cannedAcl != null) RestUtil.putSingle(headers, S3Constants.AMZ_ACL, cannedAcl.getHeaderValue());
        return headers;
    }

    @Override
    public T getEntity() {
        return getObject();
    }

    @Override
    public String getContentType() {
        return objectMetadata != null ? objectMetadata.getContentType() : null;
    }

    public S3ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    public void setObjectMetadata(S3ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }

    public T getObject() {
        return object;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    public CannedAcl getCannedAcl() {
        return cannedAcl;
    }

    public void setCannedAcl(CannedAcl cannedAcl) {
        this.cannedAcl = cannedAcl;
    }

    public PutObjectRequest withObjectMetadata(S3ObjectMetadata objectMetadata) {
        setObjectMetadata(objectMetadata);
        return this;
    }

    public PutObjectRequest withRange(Range range) {
        setRange(range);
        return this;
    }

    public PutObjectRequest withAcl(AccessControlList acl) {
        setAcl(acl);
        return this;
    }

    public PutObjectRequest withCannedAcl(CannedAcl cannedAcl) {
        setCannedAcl(cannedAcl);
        return this;
    }
}
