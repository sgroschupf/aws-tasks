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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;

import datameer.awstasks.aws.ec2.InstanceGroup;
import datameer.awstasks.aws.ec2.InstanceGroupImpl;
import datameer.awstasks.aws.emr.EmrCluster;
import datameer.awstasks.aws.emr.EmrSettings;

/**
 * Configuration class that can be used to read all ec2 access information from an properties file.
 */
public class Ec2Configuration {

    public static final String EC2_PROPERTIES_FILE = "/ec2.properties";
    private static final String ACCESS_KEY = "ec2.accessKey";
    private static final String ACCESS_KEY_SECRET = "ec2.accessSecret";
    private static final String PRIVATE_KEY_NAME = "ec2.privateKeyName";
    private static final String PRIVATE_KEY_FILE = "ec2.privateKeyFile";

    private Properties _properties;
    protected String _accessKeyId;
    protected String _accessKeySecret;
    protected String _privateKeyName;
    protected String _privateKeyFile;

    /**
     * Reads a ec2.properties from classpath. The path is {@link #EC2_PROPERTIES_FILE}.
     * 
     * @throws IOException
     */
    public Ec2Configuration() throws IOException {
        this(EC2_PROPERTIES_FILE);
    }

    /**
     * Reads the specified property files from the classpath.
     * 
     * @param files
     * @throws IOException
     */
    public Ec2Configuration(String... files) throws IOException {
        _properties = new Properties();
        for (String file : files) {
            InputStream inputStream = Ec2Configuration.class.getResourceAsStream(file);
            if (inputStream == null) {
                throw new IOException(file + " not found in classpath.");
            }
            _properties.load(inputStream);
        }
        _accessKeyId = _properties.getProperty(ACCESS_KEY);
        _accessKeySecret = _properties.getProperty(ACCESS_KEY_SECRET);
        _privateKeyName = _properties.getProperty(PRIVATE_KEY_NAME);
        _privateKeyFile = _properties.getProperty(PRIVATE_KEY_FILE);
    }

    public void resolveVariableProperties() {
        for (Object key : _properties.keySet()) {
            String keyAsString = (String) key;
            boolean retry = true;
            while (retry) {
                retry = false;
                String value = _properties.getProperty(keyAsString);
                Pattern pattern = Pattern.compile(Pattern.quote("${") + "([^}]+)" + Pattern.quote("}"));
                Matcher matcher = pattern.matcher(value);
                if (matcher.find()) {
                    String variable = matcher.group(1);
                    String valueOfVariable = System.getProperty(variable);
                    if (valueOfVariable == null) {
                        valueOfVariable = _properties.getProperty(variable);
                    }
                    if (valueOfVariable != null) {
                        value = value.replaceAll(Pattern.quote("${" + variable + "}"), Matcher.quoteReplacement(valueOfVariable));
                        _properties.setProperty(keyAsString, value);
                        retry = true;
                    }
                }
            }
        }
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

    public String getProperty(String name) {
        String property = _properties.getProperty(name);
        if (property == null) {
            throw new IllegalArgumentException("no property with name '" + name + "' configured");
        }
        return property;
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

    public AmazonS3 createS3Service() {
        return new AmazonS3Client(new BasicAWSCredentials(_accessKeyId, _accessKeySecret));
    }

    public EmrCluster createEmrCluster(String name, String s3Bucket, int instanceCount) {
        return new EmrCluster(new EmrSettings(name, _accessKeyId, _privateKeyName, s3Bucket, instanceCount), _accessKeySecret);
    }

    public InstanceGroup createInstanceGroup() {
        return createInstanceGroup(createJEc2());
    }

    public InstanceGroup createInstanceGroup(Jec2 ec2) {
        return new InstanceGroupImpl(ec2);
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
