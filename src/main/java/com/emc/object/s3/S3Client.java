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
package com.emc.object.s3;

import com.emc.object.Range;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.request.*;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * Represents all S3 operations supported by the ECS platform of the corresponding version of this library.  Note that
 * ECS does not implement all S3 operations in the API specification.  Some methods have yet to be implemented, while
 * many do not apply to a private cloud infrastructure.  ECS also extends the S3 API by providing methods not included
 * in the original specification, such as mutable objects (byte-range update) and atomic appends (returning offset).
 * <p/>
 * Any calls resulting in an error will throw S3Exception.  All available information from the error will be included in
 * the exception instance.  If an exception is not thrown, you may assume the call was successful.
 */
public interface S3Client {
    ListDataNode listDataNodes();

    /**
     * Lists the buckets owned by the user.
     */
    ListBucketsResult listBuckets();

    /**
     * List the buckets owned by the user.  ListBucketsRequest provides all available options for this call.
     */
    ListBucketsResult listBuckets(ListBucketsRequest request);

    /**
     * Returns whether a bucket exists in the user's namespace (or the configured namespace of the client). This call
     * will return true if the bucket exists even if the user does not have access to the bucket. If this call returns
     * false, a subsequent call to createBucket with the same name should succeed.
     */
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

    URL getPresignedUrl(String bucketName, String key, Date expirationTime);

    URL getPresignedUrl(PresignedUrlRequest request);

    void deleteObject(String bucketName, String key);

    void deleteVersion(String bucketName, String key, String versionId);

    DeleteObjectsResult deleteObjects(DeleteObjectsRequest request);

    void setObjectMetadata(String bucketName, String key, S3ObjectMetadata objectMetadata);

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

    ListPartsResult listParts(String bucketName, String key, String uploadId);

    ListPartsResult listParts(ListPartsRequest request);

    MultipartPartETag uploadPart(UploadPartRequest request);

    CopyPartResult copyPart(CopyPartRequest request);

    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request);

    void abortMultipartUpload(AbortMultipartUploadRequest request);
}
