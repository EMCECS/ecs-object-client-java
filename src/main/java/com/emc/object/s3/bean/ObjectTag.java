package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@XmlRootElement(name = "Tag")
@XmlType(propOrder = {"key", "value"})
public class ObjectTag {

    private String key;
    private String value;

    private final int TAG_KEY_LIMIT = 128;
    private final int TAG_VALUE_LIMIT = 256;

    private static Pattern RESTRICTED_CHARS = Pattern.compile("[$&,;\\\\?#|'<>^*()%!{}\"]"); //characters: + - = . _ : / @ are allowed

    public ObjectTag() {}

    public ObjectTag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @XmlElement(name = "Key", required = true)
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @XmlElement(name = "Value", required = true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isValidObjectTag() {

        boolean isValid = true;

        if (key == null || (key.trim().length() == 0) || value == null) {
            isValid = false;
        } else {
            if (key.length() > TAG_KEY_LIMIT || value.length() > TAG_VALUE_LIMIT) {
                isValid = false;
            } else {
                if (containsRestrictedChars(key) || containsRestrictedChars(value)) {
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    private boolean containsRestrictedChars(String tagContent) {
        Matcher matcher = RESTRICTED_CHARS.matcher(tagContent);
        return matcher.find();
    }
}