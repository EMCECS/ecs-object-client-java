package com.emc.object.s3;

import com.emc.object.s3.jersey.ErrorFilter;
import com.emc.object.util.RestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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

        Client client = ClientBuilder.newClient();
        // order of execution is reversed from this order
        client.register(new TestErrorGenerator(statusCode, xml));
        client.register(new ErrorFilter());

        try {
            client.target("http://127.0.0.1/foo").request().head();
            Assertions.fail("test error generator failed to short-circuit");
        } catch (S3Exception e) {
            Assertions.assertEquals(statusCode, e.getHttpCode());
            Assertions.assertEquals(errorCode, e.getErrorCode());
            Assertions.assertEquals(message, e.getMessage());
        }
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

        Client client = ClientBuilder.newClient();
        // order of execution is reversed from this order
        client.register(new TestErrorGenerator(statusCode, xml));
        client.register(new ErrorFilter());

        try {
            client.target("http://127.0.0.1/foo").request().head();
            Assertions.fail("test error generator failed to short-circuit");
        } catch (S3Exception e) {
            Assertions.assertEquals(statusCode, e.getHttpCode());
            Assertions.assertEquals(errorCode, e.getErrorCode());
            Assertions.assertEquals(message, e.getMessage());
        }
        client.close();
    }

    static class TestErrorGenerator implements ClientRequestFilter, ClientResponseFilter {
        private final int statusCode;
        private final String errorBody;

        TestErrorGenerator(int statusCode, String errorBody) {
            this.statusCode = statusCode;
            this.errorBody = errorBody;
        }

        @Override
        public void filter(ClientRequestContext requestContext) {
            requestContext.getHeaders().putSingle("Date", RestUtil.headerFormat(new Date()));
        }

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
            InputStream dataStream = new ByteArrayInputStream(errorBody.getBytes(StandardCharsets.UTF_8));
            responseContext.setStatusInfo(Response.Status.fromStatusCode(statusCode));
            responseContext.setEntityStream(dataStream);
        }
    }
}
