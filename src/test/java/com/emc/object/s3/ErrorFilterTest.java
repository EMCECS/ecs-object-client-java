package com.emc.object.s3;

import com.emc.object.s3.jersey.ErrorFilter;
import com.emc.object.util.RestUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.MessageBodyWorkers;
import org.junit.Assert;
import org.junit.Test;

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

        Client client = Client.create();
        // order of execution is reversed from this order
        client.addFilter(new TestErrorGenerator(statusCode, xml, client.getMessageBodyWorkers()));
        client.addFilter(new ErrorFilter());

        try {
            client.resource("http://127.0.0.1/foo").head();
            Assert.fail("test error generator failed to short-circuit");
        } catch (S3Exception e) {
            Assert.assertEquals(statusCode, e.getHttpCode());
            Assert.assertEquals(errorCode, e.getErrorCode());
            Assert.assertEquals(message, e.getMessage());
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

        Client client = Client.create();
        // order of execution is reversed from this order
        client.addFilter(new TestErrorGenerator(statusCode, xml, client.getMessageBodyWorkers()));
        client.addFilter(new ErrorFilter());

        try {
            client.resource("http://127.0.0.1/foo").head();
            Assert.fail("test error generator failed to short-circuit");
        } catch (S3Exception e) {
            Assert.assertEquals(statusCode, e.getHttpCode());
            Assert.assertEquals(errorCode, e.getErrorCode());
            Assert.assertEquals(message, e.getMessage());
        }
    }

    static class TestErrorGenerator extends ClientFilter {
        private final int statusCode;
        private final String errorBody;
        private final MessageBodyWorkers messageBodyWorkers;

        TestErrorGenerator(int statusCode, String errorBody, MessageBodyWorkers messageBodyWorkers) {
            this.statusCode = statusCode;
            this.errorBody = errorBody;
            this.messageBodyWorkers = messageBodyWorkers;
        }

        @Override
        public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
            InBoundHeaders headers = new InBoundHeaders();
            headers.putSingle("Date", RestUtil.headerFormat(new Date()));
            InputStream dataStream = new ByteArrayInputStream(errorBody.getBytes(StandardCharsets.UTF_8));
            return new ClientResponse(Response.Status.fromStatusCode(statusCode), headers, dataStream, messageBodyWorkers);
        }
    }
}
