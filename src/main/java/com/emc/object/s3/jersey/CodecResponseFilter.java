package com.emc.object.s3.jersey;

import com.emc.codec.CodecChain;
import com.emc.object.s3.S3Exception;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.jersey.SizeOverrideWriter;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Provider
@Priority(FilterPriorities.PRIORITY_CODEC_RESPONSE)
public class CodecResponseFilter implements ClientResponseFilter {

    private Map<String, Object> codecProperties;

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        Boolean encode = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_ENCODE_ENTITY);
        Map<String, String> userMeta = (Map<String, String>) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_USER_METADATA);

        if (responseContext.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            if (encode != null && encode) {
                // restore metadata from backup
                userMeta.clear();
                userMeta.putAll((Map<String, String>) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_META_BACKUP));
            }
        }
        // make sure we clear the content-length override for this thread if we set it
        if (encode != null && encode) SizeOverrideWriter.setEntitySize(null);

        // get user metadata from response headers
        MultivaluedMap<String, String> headers = responseContext.getHeaders();
        Map<String, String> storedMeta = S3ObjectMetadata.getUserMetadata(headers);
        Set<String> keysToRemove = new HashSet<String>(storedMeta.keySet());

        // get encode specs from user metadata
        String[] encodeSpecs = CodecChain.getEncodeSpecs(storedMeta);
        if (encodeSpecs != null) {

            // create codec chain
            CodecChain decodeChain = new CodecChain(encodeSpecs).withProperties(codecProperties);

            // do we need to decode the entity?
            Boolean decode = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_DECODE_ENTITY);
            if (decode != null && decode) {

                // wrap input stream with decryptor (this will remove any encode metadata from storedMeta)
                responseContext.setEntityStream(decodeChain.getDecodeStream(responseContext.getEntityStream(), storedMeta));
            } else {

                // need to remove any encode metadata so we can update the headers
                decodeChain.removeEncodeMetadata(storedMeta, decodeChain.getEncodeMetadataList(storedMeta));
            }

            // should we keep the encode headers?
            Boolean keepHeaders = (Boolean) requestContext.getConfiguration().getProperty(RestUtil.PROPERTY_KEEP_ENCODE_HEADERS);
            if (keepHeaders == null || !keepHeaders) {

                // remove encode metadata from headers (storedMeta now contains only user-defined metadata)
                keysToRemove.removeAll(storedMeta.keySet()); // all metadata - user-defined metadata
                for (String key : keysToRemove) {
                    headers.remove(S3ObjectMetadata.getHeaderName(key));
                }
            }
        }
    }

    public Map<String, Object> getCodecProperties() {
        return codecProperties;
    }

    public void setCodecProperties(Map<String, Object> codecProperties) {
        this.codecProperties = codecProperties;
    }

    public CodecResponseFilter withCodecProperties(Map<String, Object> codecProperties) {
        setCodecProperties(codecProperties);
        return this;
    }
}
