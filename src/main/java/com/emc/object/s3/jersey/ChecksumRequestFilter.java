package com.emc.object.s3.jersey;

import com.emc.object.s3.*;
import com.emc.object.util.*;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Provider
@Priority(FilterPriorities.PRIORITY_CHECKSUM_REQUEST)
public class ChecksumRequestFilter implements ClientRequestFilter {

    private S3Config s3Config;
    private S3Signer signer;
    private ThreadLocal<ClientRequestContext> requestContextThreadLocal = new ThreadLocal<>();
    private ThreadLocal<OutputStream> bufferThreadLocal = new ThreadLocal<>();

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
        Boolean generateMd5 = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_GENERATE_CONTENT_MD5);
        OutputStream out = requestContext.getEntityStream();
        bufferThreadLocal.set(out);

        if ((verifyWrite != null && verifyWrite) || (generateMd5 != null && generateMd5)){
            try {
                RunningChecksum checksum = new RunningChecksum(ChecksumAlgorithm.MD5);
                out = new ChecksummedOutputStream(out, checksum, generateMd5);
                requestContext.setEntityStream(out);
                requestContextThreadLocal.set(requestContext);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("fatal: MD5 algorithm not found");
            }
        }
    }

    private class ChecksummedOutputStream extends OutputStream {
        private OutputStream delegate;
        private RunningChecksum checksum;
        private Boolean generateContentMd5;

        public ChecksummedOutputStream(OutputStream delegate, ChecksumAlgorithm algorithm, Boolean generateContentMd5) throws NoSuchAlgorithmException {
            this(delegate, new RunningChecksum(algorithm), generateContentMd5);
        }

        public ChecksummedOutputStream(OutputStream delegate, RunningChecksum checksum, Boolean generateContentMd5) {
            this.delegate = delegate;
            this.checksum = checksum;
            this.generateContentMd5 = generateContentMd5;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b});
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            update(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();

            ClientRequestContext clientRequestContext = requestContextThreadLocal.get();
            if (generateContentMd5 != null && generateContentMd5) {
                // add Content-MD5 (before anything is written to the final stream)
                clientRequestContext.getHeaders().add(RestUtil.HEADER_CONTENT_MD5, DatatypeConverter.printBase64Binary(checksum.getByteValue()));
                // need to re-sign request because Content-MD5 is included in the signature!
                if (s3Config.getIdentity() != null) {
                    Map<String, String> parameters = RestUtil.getQueryParameterMap(clientRequestContext.getUri().getRawQuery());
                    String resource = VHostUtil.getResourceString(s3Config,
                            (String) clientRequestContext.getProperty(RestUtil.PROPERTY_NAMESPACE),
                            (String) clientRequestContext.getProperty(S3Constants.PROPERTY_BUCKET_NAME),
                            RestUtil.getEncodedPath(clientRequestContext.getUri()));
                    signer.sign(clientRequestContext, resource, parameters, clientRequestContext.getHeaders());
                }
            } else {
                clientRequestContext.setProperty(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM_VALUE, checksum.getHexValue());
            }
            clientRequestContext.setEntityStream(bufferThreadLocal.get());
        }

        public ChecksumValue getChecksum() { return checksum; }

        private void update(byte[] bytes, int offset, int length) {
            checksum.update(bytes, offset, length);
        }

    }

}
