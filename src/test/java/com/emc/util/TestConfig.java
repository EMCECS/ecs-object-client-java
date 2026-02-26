/*
 * Copyright (c) 2015, EMC Corporation.
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
package com.emc.util;

import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility functions to configure tests through a properties file located on either
 * the classpath or in the user's home directory.
 */
public class TestConfig {
    public static final String DEFAULT_PROJECT_NAME = "test"; // => $HOME/test.properties

    /**
     * Locates and loads the properties file for the test configuration.  This file can
     * reside in one of two places: somewhere in the CLASSPATH or in the user's home
     * directory.
     *
     * @return the contents of the properties file as a {@link java.util.Properties} object.
     * @throws java.io.FileNotFoundException if the file was not found
     * @throws java.io.IOException           if there was an error reading the file.
     */
    public static Properties getProperties(String projectName, boolean failIfMissing) throws IOException {
        String propFile = projectName + ".properties";
        InputStream in = TestConfig.class.getClassLoader().getResourceAsStream(propFile);
        if (in == null) {
            // Check in home directory
            File homeProps = new File(System.getProperty("user.home") + File.separator + propFile);
            if (homeProps.exists()) {
                in = new FileInputStream(homeProps);
            }
        }

        if (in == null) {
            Assumptions.assumeFalse(failIfMissing, projectName + ".properties missing (look in src/test/resources for template)");
            return null;
        }

        Properties props = new Properties();
        props.load(in);
        in.close();

        return props;
    }

    @SuppressWarnings("unused")
    public static Properties getProperties() throws IOException {
        return getProperties(DEFAULT_PROJECT_NAME, true);
    }

    /**
     * Convenience if you're using the default properties file
     */
    @SuppressWarnings("unused")
    public static String getPropertyNotEmpty(String key) throws IOException {
        return getPropertyNotEmpty(getProperties(), key);
    }

    /**
     * Utility method that gets a key from a Properties object and throws a
     * RuntimeException if the key does not exist or is not set.
     */
    public static String getPropertyNotEmpty(Properties p, String key) {
        String value = p.getProperty(key);
        Assumptions.assumeTrue(value != null && !value.isEmpty(), String.format("The property %s is required", key));
        return value;
    }
}
