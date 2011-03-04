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
        assertEquals("value-content", conf.getProperty("key"));
        assertEquals("value-content-content2", conf.getProperty("key2"));
        assertEquals("test", conf.getProperty("key3"));
        System.clearProperty("variable");
        System.clearProperty("variable2");
    }
}
