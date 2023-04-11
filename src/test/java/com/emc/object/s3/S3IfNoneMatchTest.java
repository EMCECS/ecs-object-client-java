package com.emc.object.s3;

import com.emc.object.Range;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.PutObjectRequest;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.ProcessingException;

public class S3IfNoneMatchTest extends AbstractS3ClientTest {
    @Override
    protected String getTestBucketPrefix() {
        return "if-none-match-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config());
    }

    @Test
    public void testStarAfterRangeUpdate() {
        String key = "test-star-after-range-update";

        S3ObjectMetadata metadata = new S3ObjectMetadata().withContentType("text/plain");
        PutObjectRequest ifNoneMatchRequest = new PutObjectRequest(getTestBucket(), key, "")
                .withObjectMetadata(metadata)
                .withIfNoneMatch("*");

        // create empty object
        client.putObject(ifNoneMatchRequest);

        // put range
        String data = "this is some new data";
        PutObjectRequest rangedPut = new PutObjectRequest(getTestBucket(), key, data)
                .withRange(Range.fromOffsetLength(0, data.length()));
        client.putObject(rangedPut);

        // put range again
        String moreData = "this is some additional new data";
        rangedPut = new PutObjectRequest(getTestBucket(), key, moreData)
                .withRange(Range.fromOffsetLength(data.length(), moreData.length()));
        client.putObject(rangedPut);

        // try to overwrite with IfNoneMatch*
        try {
            client.putObject(ifNoneMatchRequest);
        } catch (ProcessingException e){
            Assert.assertEquals("error not matched","At least one of the preconditions you specified did not hold.", e.getMessage());
            System.out.printf("error");
        }
    }
}
