/**
 * Copyright (C) 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.marcelsauer.jmsloadtester.tools;

import de.marcelsauer.jmsloadtester.core.JmsException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtils {

    public static Properties loadProperties(final String filename) {
        InputStream stream = PropertyUtils.class.getClassLoader().getResourceAsStream(filename);
        try {
            Properties prop = new Properties();
            prop.load(stream);
            return prop;
        } catch (IOException e) {
            throw new JmsException("could not load properties.", e);
        }
    }
}
