package com.emc.object.s3;

import com.emc.object.s3.bean.MetadataSearchDatatype;
import com.emc.object.s3.bean.MetadataSearchKey;
import com.emc.object.s3.bean.MetadataSearchList;
import com.emc.object.s3.bean.QueryObjectsResult;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.CreateBucketRequest;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.object.s3.request.QueryObjectsRequest;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests related to bucket metadata search.
 */
public class S3MetadataSearchTest extends AbstractS3ClientTest {
    private static final Logger l4j = Logger.getLogger(S3MetadataSearchTest.class);

    @Override
    protected String getTestBucketPrefix() {
        return "s3-metadata-search-test";
    }

    @Override
    public void initClient() throws Exception {
        client = new S3JerseyClient(createS3Config());
    }

    private final MetadataSearchKey[] bucketMetadataSearchKeys = new MetadataSearchKey[] {
            new MetadataSearchKey("x-amz-meta-datetime1", MetadataSearchDatatype.Datetime),
            new MetadataSearchKey("x-amz-meta-decimal1", MetadataSearchDatatype.Decimal),
            new MetadataSearchKey("x-amz-meta-integer1", MetadataSearchDatatype.Integer),
            new MetadataSearchKey("x-amz-meta-string1", MetadataSearchDatatype.String),
    };

    @Override
    protected void createBucket(String bucketName) throws Exception {
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        request.withMetadataSearchKeys(Arrays.asList(bucketMetadataSearchKeys));
        client.createBucket(request);
    }

    @Test // also tests create-with-metadata-search-keys
    public void testListBucketMetadataSearchKeys() throws Exception {

        MetadataSearchList list = client.listBucketMetadataSearchKeys(getTestBucket());
        Assert.assertNotNull(list.getIndexableKeys());
        List<MetadataSearchKey> keys = list.getIndexableKeys();
        Collections.sort(keys, new Comparator<MetadataSearchKey>() {
            @Override
            public int compare(MetadataSearchKey o1, MetadataSearchKey o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Assert.assertEquals(bucketMetadataSearchKeys.length, keys.size());
        for(int i = 0; i < bucketMetadataSearchKeys.length; i++)
        {
            MetadataSearchKey actual = keys.get(i);
            Assert.assertEquals(bucketMetadataSearchKeys[i].getName(), actual.getName());
            Assert.assertEquals(bucketMetadataSearchKeys[i].getDatatype(), actual.getDatatype());
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
                .withQuery("(x-amz-meta-string1<='') or (x-amz-meta-string1>='')");
        QueryObjectsResult result = client.queryObjects(request);

        Assert.assertEquals(bucketName, result.getBucketName());
        Assert.assertNotNull(result.getObjects());
        Assert.assertEquals(1, result.getObjects().size());
    }

}
