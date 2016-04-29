package com.emc.object.s3;

import com.emc.object.s3.bean.*;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.CreateBucketRequest;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.object.s3.request.QueryObjectsRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests related to bucket metadata search.
 */
public class S3MetadataSearchTest extends AbstractS3ClientTest {
    @Override
    protected String getTestBucketPrefix() {
        return "s3-metadata-search-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config());
    }

    private final MetadataSearchKey[] bucketMetadataSearchKeys = new MetadataSearchKey[] {
            new MetadataSearchKey("x-amz-meta-datetime1", MetadataSearchDatatype.datetime),
            new MetadataSearchKey("x-amz-meta-decimal1", MetadataSearchDatatype.decimal),
            new MetadataSearchKey("x-amz-meta-integer1", MetadataSearchDatatype.integer),
            new MetadataSearchKey("x-amz-meta-string1", MetadataSearchDatatype.string),
    };

    @Override
    protected void createBucket(String bucketName) throws Exception {
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        request.withMetadataSearchKeys(Arrays.asList(bucketMetadataSearchKeys));
        client.createBucket(request);
    }

    @Test
    public void testListSystemMetadataSearchKeys() throws Exception {

        MetadataSearchKey[] expectedIndexableKeys = new MetadataSearchKey[] {
                new MetadataSearchKey("CreateTime", MetadataSearchDatatype.datetime),
                new MetadataSearchKey("LastModified", MetadataSearchDatatype.datetime),
                new MetadataSearchKey("ObjectName", MetadataSearchDatatype.string),
                new MetadataSearchKey("Owner", MetadataSearchDatatype.string),
                new MetadataSearchKey("Size", MetadataSearchDatatype.integer),
        };

        MetadataSearchKey[] expectedOptionalAttributes = new MetadataSearchKey[] {
                new MetadataSearchKey("ContentEncoding", MetadataSearchDatatype.string),
                new MetadataSearchKey("ContentType", MetadataSearchDatatype.string),
                new MetadataSearchKey("Expiration", MetadataSearchDatatype.datetime),
                new MetadataSearchKey("Expires", MetadataSearchDatatype.datetime),
                new MetadataSearchKey("Retention", MetadataSearchDatatype.integer),
        };

        MetadataSearchList list = client.listSystemMetadataSearchKeys();
        checkMetadataKeys(expectedIndexableKeys, list.getIndexableKeys());
        checkMetadataKeys(expectedOptionalAttributes, list.getOptionalAttributes());
    }

    @Test // also tests create-with-metadata-search-keys
    public void testListBucketMetadataSearchKeys() throws Exception {

        MetadataSearchList list = client.listBucketMetadataSearchKeys(getTestBucket());
        checkMetadataKeys(bucketMetadataSearchKeys, list.getIndexableKeys());
    }

    private void checkMetadataKeys(MetadataSearchKey[] expected, List<MetadataSearchKey> actual) {
        Assert.assertNotNull(actual);
        Collections.sort(actual, new Comparator<MetadataSearchKey>() {
            @Override
            public int compare(MetadataSearchKey o1, MetadataSearchKey o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Assert.assertEquals(expected.length, actual.size());
        for(int i = 0; i < expected.length; i++)
        {
            MetadataSearchKey actualKey = actual.get(i);
            Assert.assertEquals(expected[i].getName(), actualKey.getName());
            Assert.assertEquals(expected[i].getDatatype(), actualKey.getDatatype());
        }
    }

    @Test
    public void testQueryObjects() throws Exception {
        String bucketName = getTestBucket();

        String key1 = "object1";
        Map<String, String> userMeta = new HashMap<String, String>();
        userMeta.put("datetime1", "2015-01-01T00:00:00Z");
        userMeta.put("decimal1", "3.14159");
        userMeta.put("integer1", "42");
        userMeta.put("string1", "test");

        S3ObjectMetadata objectMetadata = new S3ObjectMetadata();
        objectMetadata.setUserMetadata(userMeta);
        client.putObject(new PutObjectRequest(getTestBucket(), key1, "").withObjectMetadata(objectMetadata));

        QueryObjectsRequest request = new QueryObjectsRequest(bucketName)
                .withAttribute("ContentType")
                .withAttribute("Size")
                .withQuery("(x-amz-meta-string1<='') or (x-amz-meta-string1>='')");
        QueryObjectsResult result = client.queryObjects(request);
        Assert.assertFalse(result.isTruncated());
        Assert.assertEquals(bucketName, result.getBucketName());
        Assert.assertEquals("NO MORE PAGES", result.getNextMarker());
        Assert.assertNotNull(result.getObjects());
        Assert.assertEquals(1, result.getObjects().size());

        QueryObject obj = result.getObjects().get(0);
        Assert.assertEquals(key1, obj.getObjectName());

        Assert.assertEquals(2, obj.getQueryMds().size());
        QueryMetadata sysmd = null;
        QueryMetadata usermd = null;
        for(QueryMetadata m : obj.getQueryMds()) {
            switch(m.getType()) {
                case SYSMD: sysmd = m; break;
                case USERMD: usermd = m; break;
            }
        }
        Assert.assertNotNull(sysmd);
        Assert.assertNotNull(usermd);

        Assert.assertEquals("0", sysmd.getMdMap().get("size"));
        Assert.assertEquals("application/octet-stream", sysmd.getMdMap().get("ctype"));

        Assert.assertEquals("2015-01-01T00:00:00Z", usermd.getMdMap().get("x-amz-meta-datetime1"));
        Assert.assertEquals("3.14159", usermd.getMdMap().get("x-amz-meta-decimal1"));
        Assert.assertEquals("42", usermd.getMdMap().get("x-amz-meta-integer1"));
        Assert.assertEquals("test", usermd.getMdMap().get("x-amz-meta-string1"));
    }

}
