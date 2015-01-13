package com.emc.object.util;

import java.util.*;

public class MultivalueMap extends HashMap<String, List<Object>> {
    public MultivalueMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public MultivalueMap(int initialCapacity) {
        super(initialCapacity);
    }

    public MultivalueMap() {
    }

    public MultivalueMap(Map<? extends String, ? extends List<Object>> m) {
        super(m);
    }

    public void putSingle(String key, Object value) {
        List<Object> values = new ArrayList<>();
        values.add(value);
        put(key, values);
    }

    public void addValue(String key, Object value) {
        List<Object> values = get(key);
        if (values == null) {
            values = new ArrayList<>();
            put(key, values);
        }
        values.add(value);
    }

    public Object getFirst(String key) {
        List<Object> values = get(key);
        if (values == null || values.isEmpty()) return null;
        return values.get(0);
    }

    public String getDelimited(String key, String delimiter) {
        List<Object> values = get(key);
        if (values == null || values.isEmpty()) return null;
        StringBuilder delimited = new StringBuilder();
        Iterator<Object> valuesI = values.iterator();
        while (valuesI.hasNext()) {
            delimited.append(valuesI.next());
            if (valuesI.hasNext()) delimited.append(delimiter);
        }
        return delimited.toString();
    }
}
