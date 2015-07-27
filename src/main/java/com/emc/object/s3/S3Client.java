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
    /**
     * Always call .shutdown() when finished with a client to ensure that any attached resources and background processes
     * are released/terminated (i.e. polling threads for host list providers)
     */
    void shutdown();

    /**
     * Lists all of the data nodes in the current VDC.
     */
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
     * Returns whether <code>bucketName</code> exists in the user's namespace (or the configured namespace of the
     * client). This call will return true if the bucket exists even if the user does not have access to the bucket. If
     * this call returns false, a subsequent call to createBucket with the same name should succeed.
     */
    boolean bucketExists(String bucketName);

    /**
     * Creates a bucket with the specified name in the default namespace and with the default replication group.
     */
    void createBucket(String bucketName);

    /**
     * Creates a bucket using the parameters specified in the request object.
     *
     * @see CreateBucketRequest
     */
    void createBucket(CreateBucketRequest request);

    /**
     * Deletes <code>bucketName</code>. The bucket must be empty of all objects and versions before it can be deleted.
     */
    void deleteBucket(String bucketName);

    /**
     * Sets the specified ACL on <code>bucketName</code>
     *
     * @see AccessControlList
     */
    void setBucketAcl(String bucketName, AccessControlList acl);

    /**
     * Sets the specified canned ACL on <code>bucketName</code>
     *
     * @see CannedAcl
     */
    void setBucketAcl(String bucketName, CannedAcl cannedAcl);

    /**
     * Sets the ACL of a bucket using parameters in the request object.
     *
     * @see SetBucketAclRequest
     */
    void setBucketAcl(SetBucketAclRequest request);

    /**
     * Retrieves the ACL of <code>bucketName</code>
     *
     * @see AccessControlList
     */
    AccessControlList getBucketAcl(String bucketName);

    /**
     * Sets the CORS configuration for <code>bucketName</code>
     *
     * @see CorsConfiguration
     */
    void setBucketCors(String bucketName, CorsConfiguration corsConfiguration);

    /**
     * Retrieves the CORS configuration for <code>bucketName</code>
     *
     * @see CorsConfiguration
     */
    CorsConfiguration getBucketCors(String bucketName);

    /**
     * Removes the CORS configuration for <code>bucketName</code>
     */
    void deleteBucketCors(String bucketName);

    /**
     * Sets the lifecycle configuration for <code>bucketName</code>
     *
     * @see LifecycleConfiguration
     */
    void setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfiguration);

    /**
     * Retrieves the lifecycle configuration for <code>bucketName</code>
     *
     * @see LifecycleConfiguration
     */
    LifecycleConfiguration getBucketLifecycle(String bucketName);

    /**
     * Deletes the lifecycle configuration for <code>bucketName</code>
     */
    void deleteBucketLifecycle(String bucketName);

    /**
     * Gets the location of <code>bucketName</code>. This call will return the name of the primary VDC of the bucket
     */
    LocationConstraint getBucketLocation(String bucketName);

    /**
     * Enables or suspends versioning on <code>bucketName</code>
     */
    void setBucketVersioning(String bucketName, VersioningConfiguration versioningConfiguration);

    /**
     * Retrieves the versioning status of <code>bucketName</code> (none, enabled or suspended)
     */
    VersioningConfiguration getBucketVersioning(String bucketName);

    /**
     * Lists all objects in <code>bucketName</code> with no restrictions
     */
    ListObjectsResult listObjects(String bucketName);

    /**
     * Lists objects in <code>bucketName</code> that start with <code>prefix</code>
     */
    ListObjectsResult listObjects(String bucketName, String prefix);

    /**
     * Lists objects in a bucket using parameters specified in the request object
     */
    ListObjectsResult listObjects(ListObjectsRequest request);

    /**
     * Lists all versions of all objects in <code>bucketName</code> that start with <code>prefix</code>
     */
    ListVersionsResult listVersions(String bucketName, String prefix);

    /**
     * Lists all versions of all objects in a bucket using the parameters specified in the request object
     */
    ListVersionsResult listVersions(ListVersionsRequest request);

    /**
     * Creates or overwrites an object in <code>bucketName</code> named <code>key</code> containing <code>content</code>
     * and having <code>contentType</code>
     */
    void putObject(String bucketName, String key, Object content, String contentType);

    /**
     * Updates object <code>key</code> in bucket <code>bucketName</code> at the specified byte <code>range</code> with
     * new <code>content</code>
     */
    void putObject(String bucketName, String key, Range range, Object content);

    /**
     * Creates or updates an object using the parameters specified in the request object
     */
    PutObjectResult putObject(PutObjectRequest request);

    /**
     * Atomically appends to the end of object <code>key</code> in bucket <code>bucketName</code> with
     * <code>content</code> and returns the starting offset of the append operation
     */
    long appendObject(String bucketName, String key, Object content);

    /**
     * Remotely copies object <code>sourceKey</code> in bucket <code>sourceBucketName</code> to <code>key</code> in
     * <code>bucketName</code>
     */
    CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String bucketName, String key);

    /**
     * Remotely copies an object using the parameters specified in the request object
     */
    CopyObjectResult copyObject(CopyObjectRequest request);

    /**
     * Reads object <code>key</code> in bucket <code>bucketName</code> and converts it to <code>objectType</code>,
     * provided the conversion is supported by the implementation
     */
    <T> T readObject(String bucketName, String key, Class<T> objectType);

    /**
     * Reads version <code>versionId</code> of object <code>key</code> in bucket <code>bucketName</code> and converts
     * it to <code>objectType</code>, provided the conversion is supported by the implementation
     */
    <T> T readObject(String bucketName, String key, String versionId, Class<T> objectType);

    InputStream readObjectStream(String bucketName, String key, Range range);

    GetObjectResult<InputStream> getObject(String bucketName, String key);

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
