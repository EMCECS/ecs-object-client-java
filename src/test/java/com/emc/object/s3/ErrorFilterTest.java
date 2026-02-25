package com.emc.object.s3;

import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import com.emc.object.s3.jersey.ErrorFilter;

public class ErrorFilterTest {
    @Test
    public void testParseWithNamespace() {
        int statusCode = 500;
        String errorCode = "fooBarBazBim";
        String message = "foo bar baz bim";
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Error xmlns=\"" + S3Constants.XML_NAMESPACE + "\">" +
                "<Code>" + errorCode + "</Code>" +
                "<Message>" + message + "</Message>" +
                "<RequestId>0af69b4a:17a531ff169:46673:155</RequestId>" +
                "</Error>";

        // In Jersey 2.x, test the error parsing directly instead of through the filter chain
        S3Exception e = ErrorFilter.parseErrorResponse(new StringReader(xml), statusCode);
        Assert.assertEquals(statusCode, e.getHttpCode());
        Assert.assertEquals(errorCode, e.getErrorCode());
        Assert.assertEquals(message, e.getMessage());
    }

    @Test
    public void testParseWithoutNamespace() {
        int statusCode = 500;
        String errorCode = "fooBarBazBim";
        String message = "foo bar baz bim";
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Error>" +
                "<Code>" + errorCode + "</Code>" +
                "<Message>" + message + "</Message>" +
                "<RequestId>0af69b4a:17a531ff169:46673:155</RequestId>" +
                "</Error>";

        // In Jersey 2.x, test the error parsing directly instead of through the filter chain
        S3Exception e = ErrorFilter.parseErrorResponse(new StringReader(xml), statusCode);
        Assert.assertEquals(statusCode, e.getHttpCode());
        Assert.assertEquals(errorCode, e.getErrorCode());
        Assert.assertEquals(message, e.getMessage());
    }
}
