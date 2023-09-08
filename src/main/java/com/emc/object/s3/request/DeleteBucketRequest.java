package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class DeleteBucketRequest extends GenericBucketRequest {

    // if this bucket contains objects/versions
    private boolean backgroundDeletion;
    public DeleteBucketRequest(String bucketName, boolean backgroundDeletion) {
        super(Method.DELETE, bucketName, null);
        this.backgroundDeletion = backgroundDeletion;
    }

    public DeleteBucketRequest(String bucketName) {
        super(Method.DELETE, bucketName, null);
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (backgroundDeletion)
            RestUtil.putSingle(headers, RestUtil.EMC_EMPTY_BUCKET, "true");
        return headers;
    }

    public boolean isBackgroundDeletion() {
        return backgroundDeletion;
    }

    public void setBackgroundDeletion(boolean backgroundDeletion) {
        this.backgroundDeletion = backgroundDeletion;
    }

}
