package com.emc.object.s3;

import com.emc.object.s3.jersey.FilterPriorities;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.jersey.OctetStreamXmlProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonJaxbXMLProvider;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Priority;
import javax.ws.rs.client.*;
import javax.ws.rs.ext.Provider;
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
//        client.register(new ErrorFilter());
//        client.register(WebApplicationExceptionMapper.class);
        client.register(OctetStreamXmlProvider.class);
        client.register(JacksonJaxbXMLProvider.class);

        try {
            client.target("http://127.0.0.1/foo").request().head();
            Assert.fail("test error generator failed to short-circuit");
        } catch (S3Exception e) {
            Assert.assertEquals(statusCode,  ((S3Exception) e.getCause()).getHttpCode());
            Assert.assertEquals(errorCode, ((S3Exception) e.getCause()).getErrorCode());
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

        Client client = ClientBuilder.newClient();
        // order of execution is reversed from this order
        client.register(new TestErrorGenerator(statusCode, xml));
//        client.register(new ErrorFilter());

        try {
            client.target("http://127.0.0.1/foo").request().head();
            Assert.fail("test error generator failed to short-circuit");
        } catch (S3Exception e) {
            Assert.assertEquals(statusCode, e.getHttpCode());
            Assert.assertEquals(errorCode, e.getErrorCode());
            Assert.assertEquals(message, e.getMessage());
        }
        client.close();
    }

    @Provider
    @Priority(FilterPriorities.PRIORITY_ERROR - 1)
    static class TestErrorGenerator implements ClientResponseFilter {
        private final int statusCode;
        private final String errorBody;

        TestErrorGenerator(int statusCode, String errorBody) {
            this.statusCode = statusCode;
            this.errorBody = errorBody;
        }

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
            InputStream dataStream = new ByteArrayInputStream(errorBody.getBytes(StandardCharsets.UTF_8));
            responseContext.setStatus(statusCode);
            responseContext.getHeaders().putSingle("Date", RestUtil.headerFormat(new Date()));
            responseContext.setEntityStream(dataStream);
        }
    }
}
