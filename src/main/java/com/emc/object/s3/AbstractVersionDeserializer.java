package com.emc.object.s3;

import com.emc.object.s3.bean.AbstractVersion;
import com.emc.object.s3.bean.DeleteMarker;
import com.emc.object.s3.bean.StorageClass;
import com.emc.object.s3.bean.Version;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class AbstractVersionDeserializer extends JsonDeserializer<AbstractVersion>  {
    @Override
    public AbstractVersion deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        JsonNode etagNode = node.get("Etag");
        JsonNode sizeNode = node.get("Size");
        JsonNode storageClassNode = node.get("StorageClass");
        if (etagNode != null && sizeNode != null && storageClassNode != null) {
            return new Version(etagNode.asText(), sizeNode.asLong(), new ObjectMapper().treeToValue(storageClassNode, StorageClass.class));
        } else if (node.has("Key")) {
            return new DeleteMarker();
        } else {
            throw new JsonParseException(jp, "Could not determine subtype for AbstractVersion");
        }
    }
}
