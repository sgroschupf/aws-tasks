package com.dm.awstasks.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import com.dm.awstasks.ec2.InstanceGroup;
import com.dm.awstasks.ec2.InstanceGroupImpl;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;

/**
 * Configuration class that can be used to read all ec2 access information from an properties file.
 */
public class Ec2Configuration {

    public static final String EC2_PROPERTIES_FILE = "/ec2.properties";
    private static final String ACCESS_KEY = "ec2.accessKey";
    private static final String ACCESS_KEY_SECRET = "ec2.accessSecret";
    private static final String PRIVATE_KEY_NAME = "ec2.privateKeyName";
    private static final String PRIVATE_KEY_FILE = "ec2.privateKeyFile";

    protected String _accessKeyId;
    protected String _accessKeySecret;
    protected String _privateKeyName;
    protected String _privateKeyFile;

    /**
     * Reads a ec2.properties from classpath. The path is {@link #EC2_PROPERTIES_FILE}.
     */
    public Ec2Configuration() throws IOException {
        InputStream inputStream = Ec2Configuration.class.getResourceAsStream(EC2_PROPERTIES_FILE);
        if (inputStream == null) {
            throw new IOException(EC2_PROPERTIES_FILE + " not found in classpath");
        }
        Properties properties = new Properties();
        properties.load(inputStream);
        _accessKeyId = properties.getProperty(ACCESS_KEY);
        _accessKeySecret = properties.getProperty(ACCESS_KEY_SECRET);
        _privateKeyName = properties.getProperty(PRIVATE_KEY_NAME);
        _privateKeyFile = properties.getProperty(PRIVATE_KEY_FILE);
    }

    public boolean isEc2Configured() {
        return !nullOrEmpty(_accessKeyId, _accessKeySecret, _privateKeyName, _privateKeyFile);
    }

    public String getAccessKey() {
        return _accessKeyId;
    }

    public String getAccessSecret() {
        return _accessKeySecret;
    }

    public String getPrivateKeyName() {
        return _privateKeyName;
    }

    public String getPrivateKeyFile() {
        return _privateKeyFile;
    }

    public LaunchConfiguration createLaunchConfiguration(String ami, String group, int instanceCount) {
        LaunchConfiguration launchConfiguration = new LaunchConfiguration(ami, instanceCount, instanceCount);
        launchConfiguration.setKeyName(getPrivateKeyName());
        launchConfiguration.setSecurityGroup(Arrays.asList("default", group));
        return launchConfiguration;
    }

    public Jec2 createJEc2() {
        return new Jec2(_accessKeyId, _accessKeySecret);
    }

    public InstanceGroup createInstanceGroup() {
        return new InstanceGroupImpl(createJEc2());
    }

    private static boolean nullOrEmpty(String... strings) {
        boolean nullOrEmpty = false;
        for (String string : strings) {
            if (nullOrEmpty(string)) {
                nullOrEmpty = true;
            }
        }
        return nullOrEmpty;

    }

    private static boolean nullOrEmpty(String string) {
        return string == null || string.trim().length() == 0;
    }

}
