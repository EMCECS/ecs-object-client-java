package com.emc.object.s3;

import com.emc.object.s3.bean.AbstractDeleteResult;
import com.emc.object.s3.bean.DeleteError;
import com.emc.object.s3.bean.DeleteSuccess;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class AbstractDeleteResultDeserializer extends JsonDeserializer<AbstractDeleteResult> {
    @Override
    public AbstractDeleteResult deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode deleteMarkerNode = node.get("DeleteMarker");
        JsonNode deleteMarkerVersionIdNode = node.get("deleteMarkerVersionId");
        if (deleteMarkerNode != null && deleteMarkerVersionIdNode != null) {
            return new DeleteSuccess(deleteMarkerNode.asBoolean(), deleteMarkerVersionIdNode.asText());
        } else if (node.get("Code") != null & node.get("Message") != null) {
            return new DeleteError(node.get("Code").asText(), node.get("Message").asText());
        } else {
            throw new JsonParseException(jp, "Could not determine subtype for AbstractDeleteResult");
        }
    }
}
