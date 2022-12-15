package com.emc.object.s3.jersey;

import com.emc.object.util.*;
import com.sun.xml.bind.v2.TODO;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Provider
@Priority(FilterPriorities.PRIORITY_CHECKSUM_RESPONSE)
public class ChecksumResponseFilter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        // pull etag from response headers
        String md5Header = RestUtil.getFirstAsString(responseContext.getHeaders(), RestUtil.HEADER_ETAG);
        if (md5Header != null) md5Header = md5Header.replaceAll("\"", "");
        if (md5Header != null && (md5Header.length() <= 2 || md5Header.contains("-")))
            md5Header = null; // look for valid etags

        // also look for content MD5 (this trumps etag if present)
        String contentMd5 = RestUtil.getFirstAsString(responseContext.getHeaders(), RestUtil.EMC_CONTENT_MD5);
        if (contentMd5 != null) md5Header = contentMd5;

        Boolean verifyWrite = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM);
        if (verifyWrite != null && verifyWrite && md5Header != null) {
            // verify write checksum
            String checksumHexValue = (String) requestContext.getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM_VALUE);
            if (!checksumHexValue.equals(md5Header)) {
                throw new ChecksumError("Checksum failure while writing stream", checksumHexValue, md5Header);
            }
        }

        Boolean verifyRead = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM);
        if (verifyRead != null && verifyRead && md5Header != null) {
            // wrap stream to verify read checksum
            try {
                responseContext.setEntityStream(new ChecksummedInputStream(responseContext.getEntityStream(),
                        new ChecksumValueImpl(ChecksumAlgorithm.MD5, 0, md5Header))); // won't have length for chunked responses
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("fatal: MD5 algorithm not found");
            }
        }

    }
}
