/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
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
            if (etag != null && (etag.length() <= 2 || etag.contains("-"))) etag = null; // look for valid etags

            if (verifyWrite != null && verifyWrite && etag != null) {
                // verify write checksum
                if (!adapter.getChecksum().getValue().equals(etag))
                    throw new ChecksumError("Checksum failure while writing stream", adapter.getChecksum().getValue(), etag);
            }

            Boolean verifyRead = (Boolean) request.getProperties().get(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM);
            if (verifyRead != null && verifyRead && etag != null) {
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
                out = new ChecksummedOutputStream(out, checksum);
                return getAdapter().adapt(request, out); // don't break the chain
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("fatal: MD5 algorithm not found");
            }
        }

        public RunningChecksum getChecksum() {
            return checksum;
        }
    }
}
