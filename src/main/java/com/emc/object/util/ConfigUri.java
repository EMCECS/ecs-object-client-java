/*
 * Copyright (c) 2015-2016, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.util;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigUri<C> {
    private static final Logger log = LoggerFactory.getLogger(ConfigUri.class);
    private static final Map<Class<?>, PropertyConverter> converterClassMap = new HashMap<Class<?>, PropertyConverter>();

    static {
        converterClassMap.put(String.class, new StringPropertyConverter());
        converterClassMap.put(Integer.class, new IntegerPropertyConverter());
        converterClassMap.put(Integer.TYPE, new IntegerPropertyConverter());
        converterClassMap.put(Long.class, new LongPropertyConverter());
        converterClassMap.put(Long.TYPE, new LongPropertyConverter());
        converterClassMap.put(Float.class, new FloatPropertyConverter());
        converterClassMap.put(Float.TYPE, new FloatPropertyConverter());
        converterClassMap.put(Boolean.class, new BooleanPropertyConverter());
        converterClassMap.put(Boolean.TYPE, new BooleanPropertyConverter());
    }

    public static void registerConverter(Class<?> type, PropertyConverter converter) {
        converterClassMap.put(type, converter);
    }

    private Class<C> targetClass;
    private PropertyDescriptor protocolProperty;
    private PropertyDescriptor hostProperty;
    private PropertyDescriptor portProperty;
    private PropertyDescriptor pathProperty;
    private Map<String, PropertyDescriptor> paramPropertyMap = new TreeMap<String, PropertyDescriptor>();
    private Map<String, PropertyDescriptor> mapPropertyMap = new TreeMap<String, PropertyDescriptor>();
    private Map<PropertyDescriptor, PropertyConverter> converterMap = new HashMap<PropertyDescriptor, PropertyConverter>();

    public ConfigUri(Class<C> targetClass) {
        try {
            this.targetClass = targetClass;
            BeanInfo beanInfo = Introspector.getBeanInfo(targetClass);

            boolean annotationPresent = false;
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                if (descriptor.getReadMethod().isAnnotationPresent(ConfigUriProperty.class)) {
                    annotationPresent = true;

                    ConfigUriProperty annotation = descriptor.getReadMethod().getAnnotation(ConfigUriProperty.class);
                    switch (annotation.type()) {
                        case Protocol:
                            if (protocolProperty != null)
                                throw new IllegalArgumentException(targetClass.getSimpleName() + " has more than one Protocol binding");
                            if (descriptor.getPropertyType() != String.class && annotation.converter() == PropertyConverter.class)
                                throw new IllegalArgumentException(String.format("%s.%s property type for Protocol should be String", targetClass.getSimpleName(), descriptor.getName()));
                            protocolProperty = descriptor;
                            break;
                        case Host:
                            if (hostProperty != null)
                                throw new IllegalArgumentException(targetClass.getSimpleName() + " has more than one Host binding");
                            if (descriptor.getPropertyType() != String.class && annotation.converter() == PropertyConverter.class)
                                throw new IllegalArgumentException(String.format("%s.%s property type for Host should be String", targetClass.getSimpleName(), descriptor.getName()));
                            hostProperty = descriptor;
                            break;
                        case Port:
                            if (portProperty != null)
                                throw new IllegalArgumentException(targetClass.getSimpleName() + " has more than one Port binding");
                            if (descriptor.getPropertyType() != Integer.class && descriptor.getPropertyType() != Integer.TYPE
                                    && annotation.converter() == PropertyConverter.class)
                                throw new IllegalArgumentException(String.format("%s.%s property type for Port should be int", targetClass.getSimpleName(), descriptor.getName()));
                            portProperty = descriptor;
                            break;
                        case Path:
                            if (pathProperty != null)
                                throw new IllegalArgumentException(targetClass.getSimpleName() + " has more than one Path binding");
                            if (descriptor.getPropertyType() != String.class && annotation.converter() == PropertyConverter.class)
                                throw new IllegalArgumentException(String.format("%s.%s property type for Path should be String", targetClass.getSimpleName(), descriptor.getName()));
                            pathProperty = descriptor;
                            break;
                        case Parameter:
                            String name = isNotBlank(annotation.param()) ? annotation.param() : descriptor.getName();
                            if (descriptor.getPropertyType() == Map.class) mapPropertyMap.put(name, descriptor);
                            else paramPropertyMap.put(name, descriptor);
                            break;
                    }

                    // check for a custom property converter
                    if (annotation.converter() != PropertyConverter.class)
                        converterMap.put(descriptor, annotation.converter().newInstance());
                }
            }

            if (!annotationPresent)
                log.warn("no @ConfigUriProperty annotations found on getters in {}", targetClass.getSimpleName());
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    public C parseUri(String configUri) {
        return parseUri(configUri, false);
    }

    public C parseUri(String configUri, boolean strict) {
        try {
            URI uriObj = new URI(configUri);
            C object = targetClass.newInstance();

            if (protocolProperty != null && uriObj.getScheme() != null)
                setPropertyValues(object, protocolProperty, Collections.singletonList(uriObj.getScheme()));

            if (hostProperty != null && uriObj.getHost() != null)
                setPropertyValues(object, hostProperty, Collections.singletonList(uriObj.getHost()));

            if (portProperty != null && uriObj.getPort() > 0)
                setPropertyValues(object, portProperty, Collections.singletonList("" + uriObj.getPort()));

            if (pathProperty != null && uriObj.getPath() != null)
                setPropertyValues(object, pathProperty, Collections.singletonList(uriObj.getPath()));

            MultivaluedMap<String, String> params = getParameterMap(uriObj.getQuery());
            for (String key : params.keySet()) {

                // standard properties
                if (paramPropertyMap.containsKey(key)) {
                    PropertyDescriptor descriptor = paramPropertyMap.get(key);
                    setPropertyValues(object, descriptor, params.get(key));
                } else {

                    // map properties
                    String baseName = key.contains(".") ? key.substring(0, key.indexOf(".")) : key;
                    if (mapPropertyMap.containsKey(baseName)) {
                        if (!key.contains(".") || key.length() < baseName.length() + 2)
                            throw new IllegalArgumentException(String.format("map param %s.%s should have a period (.) and a sub-param",
                                    targetClass.getSimpleName(), key));
                        PropertyDescriptor descriptor = mapPropertyMap.get(baseName);
                        String subKey = key.substring(key.indexOf(".") + 1);
                        setMapValue(object, descriptor, subKey, params.get(key));
                    } else if (strict) {

                        // property not found on object
                        throw new IllegalArgumentException(String.format("param %s not found in class %s",
                                key, object.getClass().getSimpleName()));
                    }
                }
            }

            return object;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    public String generateUri(C object) {
        try {
            C defaultObject = targetClass.newInstance();

            // collect parameters
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();

            // standard properties
            for (Map.Entry<String, PropertyDescriptor> entry : paramPropertyMap.entrySet()) {
                String param = entry.getKey();
                List<String> values = getPropertyValues(object, entry.getValue());
                if (values == null || values.isEmpty()) continue;
                List<String> defaultValues = getPropertyValues(defaultObject, entry.getValue());
                if (!values.equals(defaultValues)) params.put(param, values);
            }

            // map properties
            for (Map.Entry<String, PropertyDescriptor> entry : mapPropertyMap.entrySet()) {
                String param = entry.getKey();
                Map<String, String> valuesMap = getValueMap(object, entry.getValue());
                if (valuesMap == null || valuesMap.isEmpty()) continue;
                Map<String, String> defaultValuesMap = getValueMap(defaultObject, entry.getValue());
                if (!valuesMap.equals(defaultValuesMap)) {
                    for (String subKey : valuesMap.keySet()) {
                        params.put(param + "." + subKey, Collections.singletonList(valuesMap.get(subKey)));
                    }
                }
            }

            String scheme = null;
            if (protocolProperty != null) {
                List<String> values = getPropertyValues(object, protocolProperty);
                if (values != null && !values.isEmpty()) scheme = values.get(0);
            }

            String host = null;
            if (hostProperty != null) {
                List<String> values = getPropertyValues(object, hostProperty);
                if (values != null && !values.isEmpty()) host = values.get(0);
            }

            int port = -1;
            if (portProperty != null) {
                List<String> values = getPropertyValues(object, portProperty);
                if (values != null && !values.isEmpty() && values.get(0) != null)
                    port = Integer.parseInt(values.get(0));
            }

            String path = null;
            if (pathProperty != null) {
                List<String> values = getPropertyValues(object, pathProperty);
                if (values != null && !values.isEmpty()) path = values.get(0);
            }

            return new URI(scheme, null, host, port, path, getQuery(params), null).toString();
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    private static final Pattern PARAM_PATTERN = Pattern.compile("^([^=]+)(?:=(.+))?$");

    private MultivaluedMap<String, String> getParameterMap(String query) {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        if (isNotBlank(query)) {
            String[] queryParts = query.split("&");
            for (String queryPart : queryParts) {
                if (isNotBlank(queryPart)) {
                    Matcher m = PARAM_PATTERN.matcher(queryPart);
                    if (!m.find()) throw new IllegalArgumentException("invalid parameter format: " + queryPart);
                    String param = m.group(1);
                    String value = (m.groupCount() > 1) ? m.group(2) : null;
                    params.add(param, value);
                }
            }
        }
        return params;
    }

    private String getQuery(MultivaluedMap<String, String> parameterMap) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : parameterMap.entrySet()) {

            // add each value as a separate parameter (key=value)
            if (entry.getValue() != null) {
                int size = query.length();
                for (String value : entry.getValue()) {
                    query.append(entry.getKey());
                    if (value != null) query.append("=").append(value);
                    query.append("&");
                }
                if (query.length() > size) query.deleteCharAt(query.length() - 1); // get rid of last ampersand

            } else {
                // no value, just the key
                query.append(entry.getKey());
            }
            query.append("&");
        }
        if (query.length() > 1) query.deleteCharAt(query.length() - 1); // get rid of last ampersand

        return query.toString();
    }

    private List<String> getPropertyValues(C object, PropertyDescriptor descriptor) {
        try {
            Object value = descriptor.getReadMethod().invoke(object);
            if (value == null) return null;
            List<String> stringValues = new ArrayList<String>();
            if (value instanceof List) {
                for (Object element : (List) value) {
                    stringValues.add(findConverter(descriptor, element.getClass()).stringFromValue(element));
                }
            } else {
                stringValues.add(findConverter(descriptor).stringFromValue(value));
            }
            return stringValues;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getValueMap(C object, PropertyDescriptor descriptor) {
        try {
            Object value = descriptor.getReadMethod().invoke(object);
            if (value == null) return null;
            if (!(value instanceof Map))
                throw new IllegalArgumentException(String.format("%s.%s is supposed to be a map, but it's not",
                        targetClass.getSimpleName(), descriptor.getName()));
            Map<String, String> valuesMap = new TreeMap<String, String>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                PropertyConverter converter = findConverter(descriptor, entry.getValue().getClass());
                valuesMap.put(entry.getKey().toString(), converter.stringFromValue(entry.getValue()));
            }
            return valuesMap;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void setPropertyValues(C object, PropertyDescriptor descriptor, List<String> stringValues) {
        try {
            synchronized (descriptor) {
                // null parameter value means null property value
                if (stringValues == null) descriptor.getWriteMethod().invoke(object, (Object) null);

                    // this framework only supports List collection type for properties
                else if (List.class == descriptor.getPropertyType()) {
                    List objects = new ArrayList();
                    for (String stringValue : stringValues) {
                        objects.add(findConverter(descriptor).valueFromString(stringValue));
                    }
                    descriptor.getWriteMethod().invoke(object, objects);

                    // single value
                } else {
                    if (stringValues.size() > 1)
                        throw new IllegalArgumentException(String.format("cannot set multiple values to property %s.%s",
                                targetClass.getSimpleName(), descriptor.getName()));
                    Object value = stringValues.isEmpty() ? null : findConverter(descriptor).valueFromString(stringValues.get(0));
                    descriptor.getWriteMethod().invoke(object, value);
                }
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void setMapValue(C object, PropertyDescriptor descriptor, String subKey, List<String> stringValues) {
        try {
            synchronized (descriptor) {
                if (Map.class != descriptor.getPropertyType())
                    throw new IllegalArgumentException(String.format("%s.%s is supposed to be a map, but it's not",
                            targetClass.getSimpleName(), descriptor.getName()));

                if (stringValues != null && stringValues.size() > 1)
                    throw new IllegalArgumentException(String.format("cannot set multiple values to map property %s.%s[%s]",
                            targetClass.getSimpleName(), descriptor.getName(), subKey));

                Map map = (Map) descriptor.getReadMethod().invoke(object);
                if (map == null) {
                    map = new HashMap();
                    descriptor.getWriteMethod().invoke(object, map);
                }

                Object value = null;
                if (stringValues != null && stringValues.size() > 0)
                    value = findConverter(descriptor).valueFromString(stringValues.get(0));

                map.put(subKey, value);
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    private PropertyConverter findConverter(PropertyDescriptor descriptor) {
        return findConverter(descriptor, descriptor.getPropertyType());
    }

    private PropertyConverter findConverter(PropertyDescriptor descriptor, Class<?> type) {
        if (converterMap.containsKey(descriptor))
            return converterMap.get(descriptor);
        if (converterClassMap.containsKey(type))
            return converterClassMap.get(type);
        throw new UnsupportedOperationException(String.format("no property converter found for %s.%s of type %s",
                targetClass.getSimpleName(), descriptor.getName(), type.getSimpleName()));
    }

    private boolean isNotBlank(String string) {
        return (string != null) && (!string.trim().isEmpty());
    }

    public interface PropertyConverter {
        Object valueFromString(String param);

        String stringFromValue(Object value);
    }

    public static class StringPropertyConverter implements PropertyConverter {
        @Override
        public Object valueFromString(String param) {
            return param;
        }

        @Override
        public String stringFromValue(Object value) {
            if (value == null) return null;
            return value.toString();
        }
    }

    public static class IntegerPropertyConverter implements PropertyConverter {
        @Override
        public Object valueFromString(String param) {
            if (param == null) return null;
            return Integer.valueOf(param);
        }

        @Override
        public String stringFromValue(Object value) {
            if (value == null) return null;
            return value.toString();
        }
    }

    public static class LongPropertyConverter implements PropertyConverter {
        @Override
        public Object valueFromString(String param) {
            if (param == null) return null;
            return Long.valueOf(param);
        }

        @Override
        public String stringFromValue(Object value) {
            if (value == null) return null;
            return value.toString();
        }
    }

    public static class FloatPropertyConverter implements PropertyConverter {
        @Override
        public Object valueFromString(String param) {
            if (param == null) return null;
            return Float.valueOf(param);
        }

        @Override
        public String stringFromValue(Object value) {
            if (value == null) return null;
            return value.toString();
        }
    }

    public static class BooleanPropertyConverter implements PropertyConverter {
        @Override
        public Object valueFromString(String param) {
            // the assumption here is that if the value is present as a parameter (even without a value), it's intended to be true
            if (param == null) return Boolean.TRUE;
            return Boolean.valueOf(param);
        }

        @Override
        public String stringFromValue(Object value) {
            if (value == null) return null;
            return value.toString();
        }
    }
}
