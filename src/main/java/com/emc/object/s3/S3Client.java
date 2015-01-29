package com.emc.object.s3;

import com.emc.object.Range;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.request.*;

import java.io.InputStream;

public interface S3Client {
    ListDataNode listDataNodes();

    /**
     * Lists the buckets owned by the configured identity
     */
    ListBucketsResult listBuckets();

    ListBucketsResult listBuckets(ListBucketsRequest request);

    boolean bucketExists(String bucketName);

    void createBucket(String bucketName);

    void createBucket(CreateBucketRequest request);

    void deleteBucket(String bucketName);

    void setBucketAcl(String bucketName, AccessControlList acl);

    void setBucketAcl(String bucketName, CannedAcl cannedAcl);

    void setBucketAcl(SetBucketAclRequest request);

    AccessControlList getBucketAcl(String bucketName);

    void setBucketCors(String bucketName, CorsConfiguration corsConfiguration);

    CorsConfiguration getBucketCors(String bucketName);

    void deleteBucketCors(String bucketName);

    void setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfiguration);

    LifecycleConfiguration getBucketLifecycle(String bucketName);

    void deleteBucketLifecycle(String bucketName);

    LocationConstraint getBucketLocation(String bucketName);

    void setBucketVersioning(String bucketName, VersioningConfiguration versioningConfiguration);

    VersioningConfiguration getBucketVersioning(String bucketName);

    ListObjectsResult listObjects(String bucketName);

    ListObjectsResult listObjects(String bucketName, String prefix);

    ListObjectsResult listObjects(ListObjectsRequest request);

    ListVersionsResult listVersions(String bucketName, String prefix);

    ListVersionsResult listVersions(ListVersionsRequest request);

    void putObject(String bucketName, String key, Object content, String contentType);

    void putObject(String bucketName, String key, Range range, Object content);

    PutObjectResult putObject(PutObjectRequest request);

    long appendObject(String bucketName, String key, Object content);

    CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String bucketName, String key);

    CopyObjectResult copyObject(CopyObjectRequest request);

    <T> T readObject(String bucketName, String key, Class<T> objectType);

    InputStream readObjectStream(String bucketName, String key, Range range);

    <T> GetObjectResult<T> getObject(GetObjectRequest request, Class<T> objectType);

    void deleteObject(String bucketName, String key);

    void deleteVersion(String bucketName, String key, String versionId);

    DeleteObjectsResult deleteObjects(DeleteObjectsRequest request);

    S3ObjectMetadata getObjectMetadata(String bucketName, String key);

    S3ObjectMetadata getObjectMetadata(GetObjectMetadataRequest request);

    void setObjectAcl(String bucketName, String key, AccessControlList acl);

    void setObjectAcl(String bucketName, String key, CannedAcl cannedAcl);

    void setObjectAcl(SetObjectAclRequest request);

    AccessControlList getObjectAcl(String bucketName, String key);

    ListMultipartUploadsResult listMultipartUploads(String bucketName);

    ListMultipartUploadsResult listMultipartUploads(ListMultipartUploadsRequest request);

    String initiateMultipartUpload(String bucketName, String key);

    InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request);

    MultipartPart uploadPart(UploadPartRequest request);

    CopyPartResult copyPart(CopyPartRequest request);

    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request);

    void abortMultipartUpload(AbortMultipartUploadRequest request);
}
