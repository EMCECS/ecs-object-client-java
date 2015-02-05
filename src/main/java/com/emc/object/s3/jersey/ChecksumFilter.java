package com.emc.object.s3.jersey;

import com.emc.object.util.*;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.filter.ClientFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

public class ChecksumFilter extends ClientFilter {
    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        try {
            ChecksumAdapter adapter = new ChecksumAdapter(request.getAdapter());

            Boolean verifyWrite = (Boolean) request.getProperties().get(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM);
            if (verifyWrite != null && verifyWrite) {
                // wrap stream to calculate write checksum
                request.setAdapter(adapter);
            }

            // execute request
            ClientResponse response = getNext().handle(request);

            // pull etag from response headers
            String etag = RestUtil.getFirstAsString(response.getHeaders(), RestUtil.HEADER_ETAG);
            if (etag != null) etag = etag.replaceAll("\"", "");

            if (verifyWrite != null && verifyWrite) {
                // verify write checksum
                if (!adapter.getChecksum().getValue().equals(etag))
                    throw new ChecksumError("Checksum failure while reading stream", adapter.getChecksum().getValue(), etag);
            }

            Boolean verifyRead = (Boolean) request.getProperties().get(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM);
            if (verifyRead != null && verifyRead) {
                // wrap stream to verify read checksum
                response.setEntityInputStream(new ChecksummedInputStream(response.getEntityInputStream(),
                        new ChecksumValueImpl(ChecksumAlgorithm.MD5, 0, etag))); // won't have length for chunked responses
            }

            return response;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("fatal: MD5 algorithm not found");
        }
    }

    private class ChecksumAdapter extends AbstractClientRequestAdapter {
        RunningChecksum checksum;

        ChecksumAdapter(ClientRequestAdapter parent) {
            super(parent);
        }

        @Override
        public OutputStream adapt(ClientRequest request, OutputStream out) throws IOException {
            try {
                checksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                return new ChecksummedOutputStream(out, checksum);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("fatal: MD5 algorithm not found");
            }
        }

        public RunningChecksum getChecksum() {
            return checksum;
        }
    }
}
