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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import datameer.awstasks.aws.ec2.InstanceGroup;
import datameer.awstasks.aws.ec2.InstanceGroupImpl;
import datameer.awstasks.aws.emr.EmrCluster;
import datameer.awstasks.aws.emr.EmrSettings;

/**
 * Configuration class that can be used to read all ec2 access information from an properties file.
 */
public class Ec2Configuration {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile(Pattern.quote("${") + "([^}]+)" + Pattern.quote("}"));
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

    @SuppressWarnings("unchecked")
    public void resolveVariableProperties() {
        List<String> keysWithPlaceholder = new ArrayList<String>();
        Enumeration<String> propertyNames = (Enumeration<String>) _properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = (String) propertyNames.nextElement();
            if (VARIABLE_PATTERN.matcher(_properties.getProperty(key)).find()) {
                keysWithPlaceholder.add(key);
            }
        }

        while (!keysWithPlaceholder.isEmpty()) {
            int resolveCount = 0;
            for (Iterator<String> iterator = keysWithPlaceholder.iterator(); iterator.hasNext();) {
                String keyWithPlaceholder = (String) iterator.next();
                String value = _properties.getProperty(keyWithPlaceholder);
                Matcher matcher = VARIABLE_PATTERN.matcher(value);
                matcher.find();
                String placeholder = matcher.group(1);
                String valueOfPlaceholder = System.getProperty(placeholder);
                if (valueOfPlaceholder == null) {
                    valueOfPlaceholder = _properties.getProperty(placeholder);
                }
                if (valueOfPlaceholder != null) {
                    resolveCount++;
                    value = value.replaceAll(Pattern.quote("${" + placeholder + "}"), Matcher.quoteReplacement(valueOfPlaceholder));
                    _properties.setProperty(keyWithPlaceholder, value);
                    if (!matcher.find()) {
                        iterator.remove();
                    }
                }
            }
            if (resolveCount == 0) {
                throw new IllegalStateException("could not resolve following keys which contain placeholders: " + keysWithPlaceholder);
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

    public RunInstancesRequest createLaunchConfiguration(String ami, String group, int instanceCount) {
        RunInstancesRequest launchConfiguration = new RunInstancesRequest(ami, instanceCount, instanceCount);
        launchConfiguration.setKeyName(getPrivateKeyName());
        launchConfiguration.setSecurityGroups(Arrays.asList("default", group));
        return launchConfiguration;
    }

    public AmazonEC2 createEc2() {
        return new AmazonEC2Client(new BasicAWSCredentials(_accessKeyId, _accessKeySecret));
    }

    public AmazonS3 createS3Service() {
        return new AmazonS3Client(new BasicAWSCredentials(_accessKeyId, _accessKeySecret));
    }

    public EmrSettings createEmrSettings(String name, String s3Bucket, int instanceCount) {
        return new EmrSettings(name, _accessKeyId, _privateKeyName, s3Bucket, instanceCount);
    }

    public EmrCluster createEmrCluster(EmrSettings settings) {
        return new EmrCluster(settings, _accessKeySecret);
    }

    public EmrCluster createEmrCluster(String name, String s3Bucket, int instanceCount) {
        EmrSettings settings = new EmrSettings(name, _accessKeyId, _privateKeyName, s3Bucket, instanceCount);
        return new EmrCluster(settings, _accessKeySecret);
    }

    @Deprecated
    public EmrCluster createEmrCluster(String name, String hadoopVersion, String s3Bucket, int instanceCount) {
        EmrSettings settings = new EmrSettings(name, _accessKeyId, _privateKeyName, s3Bucket, instanceCount);
        settings.setHadoopVersion(hadoopVersion);
        return new EmrCluster(settings, _accessKeySecret);
    }

    public InstanceGroup createInstanceGroup() {
        return createInstanceGroup(createEc2());
    }

    public InstanceGroup createInstanceGroup(AmazonEC2 ec2) {
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
