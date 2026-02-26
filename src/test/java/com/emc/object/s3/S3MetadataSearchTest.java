package com.emc.object.s3;

import com.emc.object.s3.bean.*;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.CreateBucketRequest;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.object.s3.request.QueryObjectsRequest;
import com.emc.object.util.RestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

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
            new MetadataSearchKey("ObjectName", MetadataSearchDatatype.string),
            new MetadataSearchKey("x-amz-meta-datetime1", MetadataSearchDatatype.datetime),
            new MetadataSearchKey("x-amz-meta-decimal1", MetadataSearchDatatype.decimal),
            new MetadataSearchKey("x-amz-meta-field-valid", MetadataSearchDatatype.string),
            new MetadataSearchKey("x-amz-meta-index-field", MetadataSearchDatatype.string),
            new MetadataSearchKey("x-amz-meta-integer1", MetadataSearchDatatype.integer),
            new MetadataSearchKey("x-amz-meta-key-valid", MetadataSearchDatatype.string),
            new MetadataSearchKey("x-amz-meta-string1", MetadataSearchDatatype.string)
    };

    @Override
    protected void createBucket(String bucketName) throws Exception {
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        request.withMetadataSearchKeys(Arrays.asList(bucketMetadataSearchKeys));
        client.createBucket(request);
    }

    @Test
    public void testListSystemMetadataSearchKeys() throws Exception {
        boolean is37x = ecsVersion != null && ecsVersion.matches("3\\.7\\..*");
        MetadataSearchKey[] expectedIndexableKeys;
        MetadataSearchKey[] expectedOptionalAttributes;
        if (!is37x) {
            expectedIndexableKeys = new MetadataSearchKey[]{
                    new MetadataSearchKey("CreateTime", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("LastModified", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("ObjectName", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Owner", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Size", MetadataSearchDatatype.integer),
            };

            expectedOptionalAttributes = new MetadataSearchKey[]{
                    new MetadataSearchKey("ContentEncoding", MetadataSearchDatatype.string),
                    new MetadataSearchKey("ContentType", MetadataSearchDatatype.string),
                    new MetadataSearchKey("CreateTime", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("Etag", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Expiration", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("Expires", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("LastModified", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("Namespace", MetadataSearchDatatype.string),
                    new MetadataSearchKey("ObjectName", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Owner", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Retention", MetadataSearchDatatype.integer),
                    new MetadataSearchKey("Size", MetadataSearchDatatype.integer),
            };
        }
        else {
            expectedIndexableKeys = new MetadataSearchKey[] {
                    new MetadataSearchKey("CreateTime", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("LastModified", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("ObjectName", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Owner", MetadataSearchDatatype.string),
                    new MetadataSearchKey("ReplicationStatus", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Size", MetadataSearchDatatype.integer),
            };

            expectedOptionalAttributes = new MetadataSearchKey[] {
                    new MetadataSearchKey("ContentEncoding", MetadataSearchDatatype.string),
                    new MetadataSearchKey("ContentType", MetadataSearchDatatype.string),
                    new MetadataSearchKey("CreateTime", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("Etag", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Expiration", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("Expires", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("LastModified", MetadataSearchDatatype.datetime),
                    new MetadataSearchKey("Namespace", MetadataSearchDatatype.string),
                    new MetadataSearchKey("ObjectName", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Owner", MetadataSearchDatatype.string),
                    new MetadataSearchKey("ReplicationStatus", MetadataSearchDatatype.string),
                    new MetadataSearchKey("Retention", MetadataSearchDatatype.integer),
                    new MetadataSearchKey("Size", MetadataSearchDatatype.integer),
            };
        }

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
        Assertions.assertNotNull(actual);
        Collections.sort(actual, new Comparator<MetadataSearchKey>() {
            @Override
            public int compare(MetadataSearchKey o1, MetadataSearchKey o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Assertions.assertEquals(expected.length, actual.size());
        for(int i = 0; i < expected.length; i++)
        {
            MetadataSearchKey actualKey = actual.get(i);
            Assertions.assertEquals(expected[i].getName(), actualKey.getName());
            Assertions.assertEquals(expected[i].getDatatype(), actualKey.getDatatype());
        }
    }

    @Test
    public void testObjectName() throws Exception {
        QueryObjectsRequest request = new QueryObjectsRequest(getTestBucket())
                .withQuery("ObjectName>''");
        QueryObjectsResult result = client.queryObjects(request);

        Assertions.assertEquals(getTestBucket(), result.getBucketName());
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
        client.putObject(new PutObjectRequest(getTestBucket(), key1, new byte[0]).withObjectMetadata(objectMetadata));

        QueryObjectsRequest request = new QueryObjectsRequest(bucketName)
                .withAttribute("ContentType")
                .withAttribute("Size")
                .withQuery("(x-amz-meta-string1<='') or (x-amz-meta-string1>='')");
        QueryObjectsResult result = client.queryObjects(request);

        boolean is34OrLater = ecsVersion != null && ecsVersion.compareTo("3.4") >= 0;

        Assertions.assertFalse(result.isTruncated());
        Assertions.assertEquals(bucketName, result.getBucketName());
        if (is34OrLater)
            Assertions.assertNull(result.getNextMarker());
        else
            Assertions.assertEquals("NO MORE PAGES", result.getNextMarker());
        Assertions.assertNotNull(result.getObjects());
        Assertions.assertEquals(1, result.getObjects().size());

        QueryObject obj = result.getObjects().get(0);
        Assertions.assertEquals(key1, obj.getObjectName());

        Assertions.assertEquals(2, obj.getQueryMds().size());
        QueryMetadata sysmd = null;
        QueryMetadata usermd = null;
        for(QueryMetadata m : obj.getQueryMds()) {
            switch(m.getType()) {
                case SYSMD: sysmd = m; break;
                case USERMD: usermd = m; break;
            }
        }
        Assertions.assertNotNull(sysmd);
        Assertions.assertNotNull(usermd);

        Assertions.assertEquals("0", sysmd.getMdMap().get("size"));
        Assertions.assertEquals("application/octet-stream", sysmd.getMdMap().get("ctype"));

        Assertions.assertEquals("2015-01-01T00:00:00Z", usermd.getMdMap().get("x-amz-meta-datetime1"));
        Assertions.assertEquals("3.14159", usermd.getMdMap().get("x-amz-meta-decimal1"));
        Assertions.assertEquals("42", usermd.getMdMap().get("x-amz-meta-integer1"));
        Assertions.assertEquals("test", usermd.getMdMap().get("x-amz-meta-string1"));
    }

    @Test
    public void testQueryObjectsWithPrefix() throws Exception {
        String bucketName = getTestBucket();

        String key1 = "prefix/object1";
        Map<String, String> userMeta = new HashMap<String, String>();
        userMeta.put("datetime1", "2015-01-01T00:00:00Z");
        userMeta.put("decimal1", "3.14159");
        userMeta.put("integer1", "42");
        userMeta.put("string1", "test");

        S3ObjectMetadata objectMetadata = new S3ObjectMetadata();
        objectMetadata.setUserMetadata(userMeta);
        client.putObject(new PutObjectRequest(getTestBucket(), key1, new byte[0]).withObjectMetadata(objectMetadata));

        String key2 = "object2";

        client.putObject(new PutObjectRequest(getTestBucket(), key2, new byte[0]).withObjectMetadata(objectMetadata));

        QueryObjectsRequest request = new QueryObjectsRequest(bucketName)
                .withAttribute("ContentType")
                .withAttribute("Size")
                .withQuery("(x-amz-meta-string1<='') or (x-amz-meta-string1>='')")
                .withPrefix("prefix");
        QueryObjectsResult result = client.queryObjects(request);

        boolean is34OrLater = ecsVersion != null && ecsVersion.compareTo("3.4") >= 0;
        boolean is37OrLater = ecsVersion != null && ecsVersion.compareTo("3.7") >= 0;

        Assertions.assertFalse(result.isTruncated());
        Assertions.assertEquals(bucketName, result.getBucketName());
        if (is34OrLater)
            Assertions.assertNull(result.getNextMarker());
        else
            Assertions.assertEquals("NO MORE PAGES", result.getNextMarker());
        Assertions.assertNotNull(result.getObjects());
        Assertions.assertEquals(1, result.getObjects().size());

        QueryObject obj = result.getObjects().get(0);
        Assertions.assertEquals(key1, obj.getObjectName());

        /* Blocked by STORAGE-30513 for versions before 3.7. */
        if(is37OrLater) {
            Assertions.assertEquals(2, obj.getQueryMds().size());
            QueryMetadata sysmd = null;
            QueryMetadata usermd = null;
            for (QueryMetadata m : obj.getQueryMds()) {
                switch (m.getType()) {
                    case SYSMD:
                        sysmd = m;
                        break;
                    case USERMD:
                        usermd = m;
                        break;
                }
            }
            Assertions.assertNotNull(sysmd);
            Assertions.assertNotNull(usermd);

            Assertions.assertEquals("0", sysmd.getMdMap().get("size"));
            Assertions.assertEquals("application/octet-stream", sysmd.getMdMap().get("ctype"));

            Assertions.assertEquals("2015-01-01T00:00:00Z", usermd.getMdMap().get("x-amz-meta-datetime1"));
            Assertions.assertEquals("3.14159", usermd.getMdMap().get("x-amz-meta-decimal1"));
            Assertions.assertEquals("42", usermd.getMdMap().get("x-amz-meta-integer1"));
            Assertions.assertEquals("test", usermd.getMdMap().get("x-amz-meta-string1"));
        }
    }

    @Test
    public void testQueryObjectsWithPrefixDelim() throws Exception {
        String bucketName = getTestBucket();

        String key1 = "prefix/object1";
        Map<String, String> userMeta = new HashMap<String, String>();
        userMeta.put("datetime1", "2015-01-01T00:00:00Z");
        userMeta.put("decimal1", "3.14159");
        userMeta.put("integer1", "42");
        userMeta.put("string1", "test");

        S3ObjectMetadata objectMetadata = new S3ObjectMetadata();
        objectMetadata.setUserMetadata(userMeta);
        client.putObject(new PutObjectRequest(getTestBucket(), key1, new byte[0]).withObjectMetadata(objectMetadata));

        String key2 = "prefix/prefix2/object2";
        client.putObject(new PutObjectRequest(getTestBucket(), key2, new byte[0]).withObjectMetadata(objectMetadata));

        QueryObjectsRequest request = new QueryObjectsRequest(bucketName)
                .withAttribute("ContentType")
                .withAttribute("Size")
                .withQuery("(x-amz-meta-string1<='') or (x-amz-meta-string1>='')")
                .withPrefix("prefix/")
                .withDelimiter("/");
        QueryObjectsResult result = client.queryObjects(request);

        boolean is34OrLater = ecsVersion != null && ecsVersion.compareTo("3.4") >= 0;
        boolean is37OrLater = ecsVersion != null && ecsVersion.compareTo("3.7") >= 0;

        Assertions.assertFalse(result.isTruncated());
        Assertions.assertEquals(bucketName, result.getBucketName());
        if (is34OrLater)
            Assertions.assertNull(result.getNextMarker());
        else
            Assertions.assertEquals("NO MORE PAGES", result.getNextMarker());
        Assertions.assertNotNull(result.getObjects());
        Assertions.assertEquals(1, result.getObjects().size());

        QueryObject obj = result.getObjects().get(0);
        Assertions.assertEquals(key1, obj.getObjectName());

        /* Blocked by STORAGE-30513 for versions before 3.7. */
        if(is37OrLater) {
            Assertions.assertEquals(2, obj.getQueryMds().size());
            QueryMetadata sysmd = null;
            QueryMetadata usermd = null;
            for (QueryMetadata m : obj.getQueryMds()) {
                switch (m.getType()) {
                    case SYSMD:
                        sysmd = m;
                        break;
                    case USERMD:
                        usermd = m;
                        break;
                }
            }
            Assertions.assertNotNull(sysmd);
            Assertions.assertNotNull(usermd);

            Assertions.assertEquals("0", sysmd.getMdMap().get("size"));
            Assertions.assertEquals("application/octet-stream", sysmd.getMdMap().get("ctype"));

            Assertions.assertEquals("2015-01-01T00:00:00Z", usermd.getMdMap().get("x-amz-meta-datetime1"));
            Assertions.assertEquals("3.14159", usermd.getMdMap().get("x-amz-meta-decimal1"));
            Assertions.assertEquals("42", usermd.getMdMap().get("x-amz-meta-integer1"));
            Assertions.assertEquals("test", usermd.getMdMap().get("x-amz-meta-string1"));
        }
        Assertions.assertEquals(1, result.getPrefixGroups().size());
        Assertions.assertEquals("prefix/prefix2/", result.getPrefixGroups().get(0));
        Assertions.assertFalse(result.isTruncated());
    }

    @Test
    public void testListObjectsWithEncoding() throws Exception {
        String bucketName = getTestBucket();

        String badKey = "bad\u001dkey";
        client.putObject(new PutObjectRequest(getTestBucket(), badKey, new byte[0]).withObjectMetadata(
                new S3ObjectMetadata().addUserMetadata("index-field", "bad-key")
                        .addUserMetadata("field-valid", "true")
                        .addUserMetadata("key-valid", "false")
        ));

        String goodKey = "good-key-and-field";
        client.putObject(new PutObjectRequest(getTestBucket(), goodKey, new byte[0]).withObjectMetadata(
                new S3ObjectMetadata().addUserMetadata("index-field", "good-key")
                        .addUserMetadata("field-valid", "true")
                        .addUserMetadata("key-valid", "true")
        ));

        String badField = "bad-field";
        String badFieldValue = "bad\u001dfield";
        client.putObject(new PutObjectRequest(getTestBucket(), badField, new byte[0]).withObjectMetadata(
                new S3ObjectMetadata().addEncodedUserMetadata("index-field", badFieldValue)
                        .addUserMetadata("field-valid", "false")
                        .addUserMetadata("key-valid", "true")
        ));

        // list the bad key
        QueryObjectsRequest request = new QueryObjectsRequest(bucketName).withEncodingType(EncodingType.url)
                .withQuery("(x-amz-meta-field-valid=='true') and (x-amz-meta-index-field>'')").withSorted("x-amz-meta-index-field");
        QueryObjectsResult result = client.queryObjects(request);

        Assertions.assertEquals(2, result.getObjects().size());
        Assertions.assertEquals(badKey, RestUtil.urlDecode(result.getObjects().get(0).getObjectName()));
        Assertions.assertEquals(goodKey, RestUtil.urlDecode(result.getObjects().get(1).getObjectName()));

        // list a good field, with bad field results
        request = new QueryObjectsRequest(bucketName).withEncodingType(EncodingType.url)
                .withQuery("x-amz-meta-field-valid=='false'");
        result = client.queryObjects(request);

        Assertions.assertEquals(1, result.getObjects().size());
        Assertions.assertEquals(badField, RestUtil.urlDecode(result.getObjects().get(0).getObjectName()));

        // list a bad field
        request = new QueryObjectsRequest(bucketName).withEncodingType(EncodingType.url)
                .withQuery("x-amz-meta-index-field=='" + RestUtil.urlEncode(badFieldValue) + "'");
        result = client.queryObjects(request);

        Assertions.assertEquals(1, result.getObjects().size());
        Assertions.assertEquals(badField, RestUtil.urlDecode(result.getObjects().get(0).getObjectName()));

        List<QueryMetadata> queryMds = result.getObjects().get(0).getQueryMds();

        // SYSMD and USERMD
        Assertions.assertEquals(2, queryMds.size());
        QueryMetadata usermd = null;
        for (QueryMetadata m : queryMds) {
            switch (m.getType()) {
                case USERMD:
                    usermd = m;
                    break;
            }
        }
        Assertions.assertNotNull(usermd);
        // badFieldValue has to be stored in url encoded format. Limit by SDK-553, user application needs to record encoded or not.
        Assertions.assertEquals(RestUtil.urlEncode(badFieldValue), RestUtil.urlDecode(usermd.getMdMap().get("x-amz-meta-index-field")));
        Assertions.assertEquals("false", RestUtil.urlDecode(usermd.getMdMap().get("x-amz-meta-field-valid")));
        Assertions.assertEquals("true", RestUtil.urlDecode(usermd.getMdMap().get("x-amz-meta-key-valid")));
    }

    @Test // blocked by STORAGE-30527
    public void testListObjectsWithPrefixEncoding() {
        boolean is371OrLater = ecsVersion != null && ecsVersion.compareTo("3.7.1") >= 0;
        // blocked by STORAGE-30527
        Assumptions.assumeTrue(is371OrLater, "ECS version must be at least 3.7.1. ");
        String bucketName = getTestBucket();
        String badKey = "prefix/bad\u001dkey";
        client.putObject(new PutObjectRequest(getTestBucket(), badKey, new byte[0]).withObjectMetadata(
                new S3ObjectMetadata().addUserMetadata("index-field", "bad-key")
                        .addUserMetadata("field-valid", "true")
                        .addUserMetadata("key-valid", "false")
        ));

        String goodKey = "prefix/good-key-and-field";
        client.putObject(new PutObjectRequest(getTestBucket(), goodKey, new byte[0]).withObjectMetadata(
                new S3ObjectMetadata().addUserMetadata("index-field", "good-key")
                        .addUserMetadata("field-valid", "true")
                        .addUserMetadata("key-valid", "true")
        ));

        String badField = "prefix/bad-field";
        String badFieldValue = "bad\u001dfield";
        client.putObject(new PutObjectRequest(getTestBucket(), badField, new byte[0]).withObjectMetadata(
                new S3ObjectMetadata().addEncodedUserMetadata("index-field", badFieldValue)
                        .addUserMetadata("field-valid", "false")
                        .addUserMetadata("key-valid", "true")
        ));

        QueryObjectsRequest request = null;
        QueryObjectsResult result = null;
        // list the bad key with wrong prefix
        request = new QueryObjectsRequest(bucketName).withEncodingType(EncodingType.url)
                .withQuery("(x-amz-meta-field-valid=='true') and (x-amz-meta-index-field>'')")
                .withPrefix("prefix1/");
        result = client.queryObjects(request);

        Assertions.assertEquals(0, result.getObjects().size());

        // list the bad key
        request = new QueryObjectsRequest(bucketName).withEncodingType(EncodingType.url)
                .withQuery("(x-amz-meta-field-valid=='true') and (x-amz-meta-index-field>'')")
                .withPrefix("prefix/");
        result = client.queryObjects(request);

        Assertions.assertEquals(2, result.getObjects().size());
        Assertions.assertEquals(badKey, RestUtil.urlDecode(result.getObjects().get(0).getObjectName()));
        Assertions.assertEquals(goodKey, RestUtil.urlDecode(result.getObjects().get(1).getObjectName()));

        // list a good field, with bad field results
        request = new QueryObjectsRequest(bucketName).withEncodingType(EncodingType.url)
                .withQuery("x-amz-meta-field-valid=='false'").withPrefix("prefix/");
        result = client.queryObjects(request);

        Assertions.assertEquals(1, result.getObjects().size());
        Assertions.assertEquals(badField, RestUtil.urlDecode(result.getObjects().get(0).getObjectName()));

        // list a bad field
        request = new QueryObjectsRequest(bucketName).withEncodingType(EncodingType.url)
                .withQuery("x-amz-meta-index-field=='" + RestUtil.urlEncode(badFieldValue) + "'")
                .withPrefix("prefix/");
        result = client.queryObjects(request);

        Assertions.assertEquals(1, result.getObjects().size());
        Assertions.assertEquals(badField, RestUtil.urlDecode(result.getObjects().get(0).getObjectName()));

        List<QueryMetadata> queryMds = result.getObjects().get(0).getQueryMds();

        // SYSMD and USERMD
        Assertions.assertEquals(2, queryMds.size());
        QueryMetadata usermd = null;
        for (QueryMetadata m : queryMds) {
            switch (m.getType()) {
                case USERMD:
                    usermd = m;
                    break;
            }
        }
        Assertions.assertNotNull(usermd);
        // badFieldValue has to be stored in url encoded format. Limit by SDK-553, user application needs to record encoded or not.
        Assertions.assertEquals(RestUtil.urlEncode(badFieldValue), RestUtil.urlDecode(usermd.getMdMap().get("x-amz-meta-index-field")));
        Assertions.assertEquals("false", RestUtil.urlDecode(usermd.getMdMap().get("x-amz-meta-field-valid")));
        Assertions.assertEquals("true", RestUtil.urlDecode(usermd.getMdMap().get("x-amz-meta-key-valid")));
    }

    @Test
    public void testCaseSensitivity() throws Exception {
        String bucketName = getTestBucket();

        String key1 = "object1";
        Map<String, String> userMeta = new HashMap<String, String>();
        userMeta.put("dAtetIme1", "2015-01-01T00:00:00Z");
        userMeta.put("decImal1", "3.14159");
        userMeta.put("intEger1", "42");
        userMeta.put("strIng1", "test");

        S3ObjectMetadata objectMetadata = new S3ObjectMetadata();
        objectMetadata.setUserMetadata(userMeta);
        client.putObject(new PutObjectRequest(getTestBucket(), key1, new byte[0]).withObjectMetadata(objectMetadata));

        // verify all UMD is stored lowercase
        objectMetadata = client.getObjectMetadata(getTestBucket(), key1);
        Assertions.assertNotNull(objectMetadata.getUserMetadata("datetime1"));
        Assertions.assertNotNull(objectMetadata.getUserMetadata("decimal1"));
        Assertions.assertNotNull(objectMetadata.getUserMetadata("integer1"));
        Assertions.assertNotNull(objectMetadata.getUserMetadata("string1"));

        // test case-insensitive search
        QueryObjectsRequest request = new QueryObjectsRequest(bucketName)
                .withAttribute("ContentType")
                .withAttribute("Size")
                .withQuery("(x-amz-meta-STRING1<='') or (x-amz-meta-STRING1>='')");
        QueryObjectsResult result = client.queryObjects(request);

        boolean is34OrLater = ecsVersion != null && ecsVersion.compareTo("3.4") >= 0;

        Assertions.assertFalse(result.isTruncated());
        Assertions.assertEquals(bucketName, result.getBucketName());
        if (is34OrLater)
            Assertions.assertNull(result.getNextMarker());
        else
            Assertions.assertEquals("NO MORE PAGES", result.getNextMarker());
        Assertions.assertNotNull(result.getObjects());
        Assertions.assertEquals(1, result.getObjects().size());

        QueryObject obj = result.getObjects().get(0);
        Assertions.assertEquals(key1, obj.getObjectName());

        Assertions.assertEquals(2, obj.getQueryMds().size());
        QueryMetadata sysmd = null;
        QueryMetadata usermd = null;
        for(QueryMetadata m : obj.getQueryMds()) {
            switch(m.getType()) {
                case SYSMD: sysmd = m; break;
                case USERMD: usermd = m; break;
            }
        }
        Assertions.assertNotNull(sysmd);
        Assertions.assertNotNull(usermd);

        Assertions.assertEquals("0", sysmd.getMdMap().get("size"));
        Assertions.assertEquals("application/octet-stream", sysmd.getMdMap().get("ctype"));

        Assertions.assertEquals("2015-01-01T00:00:00Z", usermd.getMdMap().get("x-amz-meta-datetime1"));
        Assertions.assertEquals("3.14159", usermd.getMdMap().get("x-amz-meta-decimal1"));
        Assertions.assertEquals("42", usermd.getMdMap().get("x-amz-meta-integer1"));
        Assertions.assertEquals("test", usermd.getMdMap().get("x-amz-meta-string1"));
    }
}
