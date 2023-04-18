package com.emc.object.s3;

import com.emc.object.s3.jersey.ErrorFilter;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

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
        client.register(new ErrorFilter());

        // as ConnectException could not be passed by ClientResponseFilter in Jersey 2.x,
        // basically we cannot generate a specific error by defining any customized filter to test the functionality of ErrorFilter.
        WireMockServer wireMockServer = new WireMockServer(options().port(8080));
        wireMockServer.start();
        stubFor(any(urlEqualTo("/foo")).willReturn(aResponse()
                .withStatus(statusCode)
                .withStatusMessage(message)
                .withHeader("Content-Type", "application/xml")
                .withBody(xml.getBytes(StandardCharsets.UTF_8))));

        try {
            // Note that head() is not working here, cause Jersey 2.x would swallow the response body.
            // Then ErrorFilter will have nothing to parse. So we got to use get().
            client.target("http://127.0.0.1:8080/foo").request().get();
            Assert.fail("test error generator failed to short-circuit");
        } catch (RuntimeException e) {
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
        client.register(new ErrorFilter());

        // as above
        WireMockServer wireMockServer = new WireMockServer(options().port(8080));
        wireMockServer.start();
        stubFor(any(urlEqualTo("/foo")).willReturn(aResponse()
                .withStatus(statusCode)
                .withStatusMessage(message)
                .withHeader("Content-Type", "application/xml")
                .withBody(xml.getBytes(StandardCharsets.UTF_8))));

        try {
            // as above
            client.target("http://127.0.0.1:8080/foo").request().get();
            Assert.fail("test error generator failed to short-circuit");
        } catch (ProcessingException e) {
            Assert.assertEquals(statusCode, ((S3Exception) e.getCause()).getHttpCode());
            Assert.assertEquals(errorCode, ((S3Exception) e.getCause()).getErrorCode());
            Assert.assertEquals(message, e.getMessage());
        }
        client.close();
        wireMockServer.stop();
    }

}
