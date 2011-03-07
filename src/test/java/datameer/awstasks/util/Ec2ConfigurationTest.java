/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datameer.awstasks.util;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class Ec2ConfigurationTest {

    @Test
    public void Ec2ConfigurationReadFromMultiplePropertyFiles() throws IOException {
        Ec2Configuration conf = new Ec2Configuration("/file1.properties", "/file2.properties");
        assertEquals("accessKey", conf.getAccessKey());
        assertEquals("secret", conf.getAccessSecret());
    }

    @Test
    public void testResolveVariableProperties() throws IOException {
        System.setProperty("variable", "content");
        System.setProperty("variable2", "content2");
        Ec2Configuration conf = new Ec2Configuration("/file3.properties");
        conf.resolveVariableProperties();
        assertEquals("value-content", conf.getProperty("key1"));
        assertEquals("value-content-content2", conf.getProperty("key2"));
        assertEquals("test", conf.getProperty("key3"));
        assertEquals("value-value-content", conf.getProperty("key0"));
        System.clearProperty("variable");
        System.clearProperty("variable2");
        System.out.println(conf.getProperty("key0"));
    }

    @Test
    public void testResolveVariableProperties_CouldNotResolve() throws IOException {
        System.setProperty("variable", "content");
        Ec2Configuration conf = new Ec2Configuration("/file3.properties");

        try {
            conf.resolveVariableProperties();
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
    }

}
