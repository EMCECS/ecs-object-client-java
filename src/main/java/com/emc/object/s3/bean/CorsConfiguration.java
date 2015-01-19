package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlRootElement(name = "CORSConfiguration")
public class CorsConfiguration {
    private List<CorsRule> corsRules = new ArrayList<>();

    @XmlElement(name = "CORSRule")
    public List<CorsRule> getCorsRules() {
        return corsRules;
    }

    public void setCorsRules(List<CorsRule> corsRules) {
        this.corsRules = corsRules;
    }

    public CorsConfiguration withCorsRules(List<CorsRule> corsRules) {
        setCorsRules(corsRules);
        return this;
    }

    public CorsConfiguration withCorsRules(CorsRule... corsRules) {
        return withCorsRules(Arrays.asList(corsRules));
    }
}
