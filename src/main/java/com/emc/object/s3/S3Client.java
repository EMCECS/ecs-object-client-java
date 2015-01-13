package com.emc.object.s3;

import com.emc.object.s3.bean.*;
import com.emc.object.s3.request.ListObjectsRequest;

public interface S3Client {
    /**
     * Lists the buckets owned by the configured identity
     */
    ListBucketsResult listBuckets();

    void deleteBucket(String bucketName);

    void deleteBucketCors(String bucketName);

    void deleteBucketLifecycle(String bucketName);

    ListObjectsResult listObjects(ListObjectsRequest request);

    ListObjectsResult listObjects(String bucketName);

    ListObjectsResult listObjects(String bucketName, String prefix);

    CorsConfiguration getBucketCors(String bucketName);

    AccessControlList getBucketAcl(String bucketName);

    LifecycleConfiguration getBucketLifecycle(String bucketName);

    boolean bucketExists(String bucketName);

    void createBucket(String bucketName);

    // TODO: other forms of create bucket

    void setBucketCors(String bucketName, CorsConfiguration corsConfiguration);

    void setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfiguration);
}
