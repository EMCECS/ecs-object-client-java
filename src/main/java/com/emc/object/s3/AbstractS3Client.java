package com.emc.object.s3;

import com.emc.object.AbstractJerseyClient;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.request.*;

public abstract class AbstractS3Client extends AbstractJerseyClient implements S3Client {
    protected S3Config s3Config;

    protected AbstractS3Client(S3Config s3Config) {
        super(s3Config);
        this.s3Config = s3Config;
    }

    @Override
    public ListBucketsResult listBuckets() {
        return listBuckets(new ListBucketsRequest());
    }

    @Override
    public void createBucket(String bucketName) {
        createBucket(new CreateBucketRequest().withBucketName(bucketName));
    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList acl) {
        setBucketAcl(new SetBucketAclRequest().withAcl(acl));
    }

    @Override
    public void setBucketAcl(String bucketName, CannedAcl cannedAcl) {
        setBucketAcl(new SetBucketAclRequest().withCannedAcl(cannedAcl));
    }

    @Override
    public ListObjectsResult listObjects(String bucketName) {
        return listObjects(new ListObjectsRequest().withBucketName(bucketName));
    }

    @Override
    public ListObjectsResult listObjects(String bucketName, String prefix) {
        return listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix));
    }

    @Override
    public ListVersionsResult listVersions(String bucketName, String prefix) {
        return listVersions(new ListVersionsRequest().withBucketName(bucketName).withPrefix(prefix));
    }

    @Override
    public ListMultipartUploadsResult listMultipartUploads(String bucketName) {
        return listMultipartUploads(new ListMultipartUploadsRequest().withBucketName(bucketName));
    }
}
