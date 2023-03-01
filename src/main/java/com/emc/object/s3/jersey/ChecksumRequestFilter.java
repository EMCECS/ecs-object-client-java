package com.emc.object.s3.jersey;

import com.emc.object.s3.*;
import com.emc.object.util.RestUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Map;

@Provider
@Priority(FilterPriorities.PRIORITY_CHECKSUM_REQUEST)
public class ChecksumRequestFilter implements ClientRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ChecksumRequestFilter.class);

    private S3Config s3Config;
    private S3Signer signer;
    ThreadLocal<MessageDigest> MD5_DIGEST = ThreadLocal.withInitial(DigestUtils::getMd5Digest);

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
            // todo wrap stream to calculate write checksum
        }

        Boolean generateMd5 = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_GENERATE_CONTENT_MD5);
        if (generateMd5 != null && generateMd5) {
            // wrap stream to generate Content-MD5 header
            OutputStream outputStream = requestContext.getEntityStream();
            outputStream = new BufferedOutputStream(outputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.writeTo(outputStream);

            final MessageDigest md5Digest = MD5_DIGEST.get();
            md5Digest.reset();
            final byte[] contentMd5Bytes = md5Digest.digest(baos.toByteArray());
            final String contentMd5EncodedHash = new String(Base64.encodeBase64(contentMd5Bytes));

            // set the Content-MD5 header in the request
            requestContext.getHeaders().add(RestUtil.HEADER_CONTENT_MD5, contentMd5EncodedHash);

        }

        if (s3Config.getIdentity() != null) {
            Map<String, String> parameters = RestUtil.getQueryParameterMap(requestContext.getUri().getRawQuery());

            String resource = VHostUtil.getResourceString(s3Config,
                    (String) requestContext.getProperty(RestUtil.PROPERTY_NAMESPACE),
                    (String) requestContext.getProperty(S3Constants.PROPERTY_BUCKET_NAME),
                    RestUtil.getEncodedPath(requestContext.getUri()));

            signer.sign(requestContext,
                    resource,
                    parameters,
                    requestContext.getHeaders());
        }
    }
}
