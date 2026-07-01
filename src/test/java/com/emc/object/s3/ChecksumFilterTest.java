/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.Future;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.emc.object.s3.jersey.ChecksumFilter;
import com.emc.object.util.ChecksumAlgorithm;
import com.emc.object.util.ChecksumError;
import com.emc.object.util.ChecksummedInputStream;
import com.emc.object.util.RestUtil;
import com.emc.object.util.RunningChecksum;

import static org.mockito.Mockito.*;

public class ChecksumFilterTest {

    private static final String PROP_WRITE_CHECKSUM = "com.emc.object.checksumFilter.writeChecksum";

    // -----------------------------------------------------------------------
    // Unit tests: each filter method tested in isolation via mocks
    // -----------------------------------------------------------------------

    @Test
    public void testWriteInterceptorWrapsStreamAndStoresChecksum() throws Exception {
        WriterInterceptorContext ctx = mock(WriterInterceptorContext.class);
        when(ctx.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM)).thenReturn(Boolean.TRUE);
        when(ctx.getProperty(RestUtil.PROPERTY_GENERATE_CONTENT_MD5)).thenReturn(null);
        when(ctx.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        new ChecksumFilter(new S3Config()).aroundWriteTo(ctx);

        // the output stream must be wrapped with a checksumming stream
        ArgumentCaptor<OutputStream> streamCaptor = ArgumentCaptor.forClass(OutputStream.class);
        verify(ctx).setOutputStream(streamCaptor.capture());
        Assertions.assertNotNull(streamCaptor.getValue());

        // the running checksum must be stored so the response filter can compare it
        verify(ctx).setProperty(eq(PROP_WRITE_CHECKSUM), any(RunningChecksum.class));

        // the chain must be continued
        verify(ctx).proceed();
    }

    @Test
    public void testResponseFilterPassesOnCorrectWriteChecksum() throws Exception {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String correctMd5 = DigestUtils.md5Hex(data);

        RunningChecksum storedChecksum = new RunningChecksum(ChecksumAlgorithm.MD5);
        storedChecksum.update(data, 0, data.length);

        ClientRequestContext reqCtx = mock(ClientRequestContext.class);
        ClientResponseContext respCtx = mock(ClientResponseContext.class);
        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(RestUtil.EMC_CONTENT_MD5, correctMd5);

        when(reqCtx.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM)).thenReturn(Boolean.TRUE);
        when(reqCtx.getProperty(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM)).thenReturn(null);
        when(reqCtx.getProperty(PROP_WRITE_CHECKSUM)).thenReturn(storedChecksum);
        when(respCtx.getHeaders()).thenReturn(headers);

        // should not throw when checksum matches
        new ChecksumFilter(new S3Config()).filter(reqCtx, respCtx);
    }

    @Test
    public void testResponseFilterThrowsOnWriteChecksumMismatch() throws Exception {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);

        RunningChecksum storedChecksum = new RunningChecksum(ChecksumAlgorithm.MD5);
        storedChecksum.update(data, 0, data.length);

        ClientRequestContext reqCtx = mock(ClientRequestContext.class);
        ClientResponseContext respCtx = mock(ClientResponseContext.class);
        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(RestUtil.EMC_CONTENT_MD5, "abcdef0123456789abcdef0123456789");

        when(reqCtx.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM)).thenReturn(Boolean.TRUE);
        when(reqCtx.getProperty(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM)).thenReturn(null);
        when(reqCtx.getProperty(PROP_WRITE_CHECKSUM)).thenReturn(storedChecksum);
        when(respCtx.getHeaders()).thenReturn(headers);

        Assertions.assertThrows(ChecksumError.class,
                () -> new ChecksumFilter(new S3Config()).filter(reqCtx, respCtx));
    }

    @Test
    public void testResponseFilterWrapsEntityStreamForReadVerification() throws Exception {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String correctMd5 = DigestUtils.md5Hex(data);

        ClientRequestContext reqCtx = mock(ClientRequestContext.class);
        ClientResponseContext respCtx = mock(ClientResponseContext.class);
        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(RestUtil.HEADER_ETAG, correctMd5);

        when(reqCtx.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM)).thenReturn(null);
        when(reqCtx.getProperty(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM)).thenReturn(Boolean.TRUE);
        when(respCtx.getHeaders()).thenReturn(headers);
        when(respCtx.getEntityStream()).thenReturn(new ByteArrayInputStream(data));

        new ChecksumFilter(new S3Config()).filter(reqCtx, respCtx);

        // the response stream must be wrapped with a ChecksummedInputStream
        ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(respCtx).setEntityStream(streamCaptor.capture());
        Assertions.assertInstanceOf(ChecksummedInputStream.class, streamCaptor.getValue());
    }

    // -----------------------------------------------------------------------
    // End-to-end tests: real Jersey client pipeline proves that the checksum
    // stored by aroundWriteTo() via WriterInterceptorContext.setProperty()
    // propagates to filter() via ClientRequestContext.getProperty().
    // -----------------------------------------------------------------------

    @Test
    public void testEndToEndWriteChecksumThrowsOnMismatch() throws Exception {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String wrongMd5 = "abcdef0123456789abcdef0123456789";

        Client client = ClientBuilder.newClient(
                new ClientConfig().connectorProvider(new MockConnector(wrongMd5)));
        client.register(new ChecksumFilter(new S3Config()));
        try {
            client.target("http://localhost/test")
                    .request()
                    .property(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM, Boolean.TRUE)
                    .put(Entity.entity(data, "application/octet-stream"));
            Assertions.fail("expected ChecksumError wrapped in ProcessingException");
        } catch (ProcessingException e) {
            Assertions.assertInstanceOf(ChecksumError.class, e.getCause(),
                    "root cause must be a ChecksumError, was: " + e.getCause());
        } finally {
            client.close();
        }
    }

    @Test
    public void testEndToEndWriteChecksumPassesOnMatch() throws Exception {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        String correctMd5 = DigestUtils.md5Hex(data);

        Client client = ClientBuilder.newClient(
                new ClientConfig().connectorProvider(new MockConnector(correctMd5)));
        client.register(new ChecksumFilter(new S3Config()));
        try {
            Response response = client.target("http://localhost/test")
                    .request()
                    .property(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM, Boolean.TRUE)
                    .put(Entity.entity(data, "application/octet-stream"));
            Assertions.assertEquals(200, response.getStatus());
        } finally {
            client.close();
        }
    }

    // -----------------------------------------------------------------------
    // Mock connector: acts as an in-process "server" so tests run without
    // a real HTTP endpoint. Calling request.writeEntity() drains the entity
    // through the full WriterInterceptor chain (including ChecksumFilter),
    // then a fake response is returned with a caller-supplied MD5 header.
    // -----------------------------------------------------------------------

    private static class MockConnector implements Connector, ConnectorProvider {
        private final String responseMd5;

        MockConnector(String responseMd5) {
            this.responseMd5 = responseMd5;
        }

        @Override
        public Connector getConnector(Client client, Configuration runtimeConfig) {
            return this;
        }

        @Override
        public ClientResponse apply(ClientRequest request) throws ProcessingException {
            // Provide a sink stream and run the WriterInterceptor chain; this is
            // where ChecksumFilter.aroundWriteTo() stores the RunningChecksum into
            // the request property bag via WriterInterceptorContext.setProperty().
            request.setStreamProvider(contentLength -> new ByteArrayOutputStream());
            try {
                request.writeEntity();
            } catch (IOException e) {
                throw new ProcessingException(e);
            }

            // Return a fake 200 response carrying the configured MD5 header.
            // ChecksumFilter.filter() will pick it up from ClientRequestContext
            // (same property bag) and compare against the stored checksum.
            ClientResponse response = new ClientResponse(Response.Status.OK, request);
            response.headers(RestUtil.EMC_CONTENT_MD5, responseMd5);
            response.setEntityStream(new ByteArrayInputStream(new byte[0]));
            return response;
        }

        @Override
        public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
            throw new UnsupportedOperationException("async not supported by MockConnector");
        }

        @Override
        public String getName() { return "MockConnector"; }

        @Override
        public void close() { }
    }
}
