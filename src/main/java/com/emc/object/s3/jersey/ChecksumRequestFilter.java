package com.emc.object.s3.jersey;

import com.emc.object.s3.*;
import com.emc.object.util.ChecksumAlgorithm;
import com.emc.object.util.ChecksummedOutputStream;
import com.emc.object.util.RestUtil;
import com.emc.object.util.RunningChecksum;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Provider
@Priority(FilterPriorities.PRIORITY_CHECKSUM_REQUEST)
public class ChecksumRequestFilter implements ClientRequestFilter {

    private static S3Config s3Config;
    private static S3Signer signer;
    private static RunningChecksum checksum;
    private static final ThreadLocal<ClientRequestContext> requestContextThreadLocal = new ThreadLocal<>();
    private static ByteArrayOutputStream buffer;
    private static OutputStream finalStream;

    public ChecksumRequestFilter(S3Config s3Config) {
        this.s3Config = s3Config;
        if(s3Config.isUseV2Signer())
            this.signer = new S3SignerV2(s3Config);
        else
            this.signer = new S3SignerV4(s3Config);
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        Boolean verifyWrite = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM);
        if (verifyWrite != null && verifyWrite) {
            // wrap stream to calculate write checksum
            try {
                requestContext.setEntityStream(new ChecksummedOutputStream(requestContext.getEntityStream(), new RunningChecksum(ChecksumAlgorithm.MD5)));
                requestContextThreadLocal.set(requestContext);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("fatal: MD5 algorithm not found");
            }
        }

        Boolean generateMd5 = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_GENERATE_CONTENT_MD5);
        if (generateMd5 != null && generateMd5) {
            // wrap stream to generate Content-MD5 header
            buffer = new ByteArrayOutputStream();
            OutputStream out = requestContext.getEntityStream();
            finalStream = out;
            try {
                checksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                out = new CloseNotifyOutputStream(buffer);
                out = new ChecksummedOutputStream(out, checksum);
                requestContext.setEntityStream(out);
                requestContextThreadLocal.set(requestContext);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("fatal: MD5 algorithm not found");
            }
        }

    }

    private static class CloseNotifyOutputStream extends FilterOutputStream {
        CloseNotifyOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            super.close();
            ClientRequestContext clientRequestContext = requestContextThreadLocal.get();
            // add Content-MD5 (before anything is written to the final stream)
            clientRequestContext.getHeaders().add(RestUtil.HEADER_CONTENT_MD5, DatatypeConverter.printBase64Binary(checksum.getByteValue()));

            // need to re-sign request because Content-MD5 is included in the signature!
            if (s3Config.getIdentity() != null) {
                Map<String, String> parameters = RestUtil.getQueryParameterMap(clientRequestContext.getUri().getRawQuery());

                String resource = VHostUtil.getResourceString(s3Config,
                        (String) clientRequestContext.getProperty(RestUtil.PROPERTY_NAMESPACE),
                        (String) clientRequestContext.getProperty(S3Constants.PROPERTY_BUCKET_NAME),
                        RestUtil.getEncodedPath(clientRequestContext.getUri()));

                signer.sign(clientRequestContext,
                        resource,
                        parameters,
                        clientRequestContext.getHeaders());
            }

            // write the complete buffered data
            finalStream.write(buffer.toByteArray());
        }
    }

}
