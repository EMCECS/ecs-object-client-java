package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class DeleteBucketRequest extends GenericBucketRequest {

    /**
     * trigger to use the new bucket deletion feature (only supported on ECS version 3.8 or above)
     */
    private boolean recursivelyDeleteAllData;
    public DeleteBucketRequest(String bucketName, boolean recursivelyDeleteAllData) {
        super(Method.DELETE, bucketName, null);
        this.recursivelyDeleteAllData = recursivelyDeleteAllData;
    }

    public DeleteBucketRequest(String bucketName) {
        super(Method.DELETE, bucketName, null);
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (recursivelyDeleteAllData)
            RestUtil.putSingle(headers, RestUtil.EMC_EMPTY_BUCKET, "true");
        return headers;
    }

    public boolean isRecursivelyDeleteAllData() {
        return recursivelyDeleteAllData;
    }

    /**
     * The current delete operation is a synchronous operation, when recursivelyDeleteAllData is default set to false..
     * When recursivelyDeleteAllData comes to true, the request becomes asynchronous and will return immediately.
     * The bucket will be marked as "delete in progress". The bucket will be read-only through S3 and will not allow reads/writes through NFS (for FS bucket).
     * Background tasks will be initiated across all of the associated zones and tables to remove objects/versions associated with the buckets.
     * Once all tasks have completed the bucket will be deleted from the system.
     * It must be noted that the issue of object lock and governance must not be overlooked for such an operation.
     * It must be noted that the operation will delete all objects and data in the bucket and it cannot be undone.
     * @param recursivelyDeleteAllData trigger to use the new bucket deletion feature (only supported on ECS version 3.8 or above)
     */
    public void setRecursivelyDeleteAllData(boolean recursivelyDeleteAllData) {
        this.recursivelyDeleteAllData = recursivelyDeleteAllData;
    }

}
