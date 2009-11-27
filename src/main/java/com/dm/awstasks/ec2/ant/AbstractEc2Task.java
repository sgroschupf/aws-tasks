package com.dm.awstasks.ec2.ant;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Task;

public class AbstractEc2Task extends Task {

    protected static final Logger LOG = Logger.getLogger(AbstractEc2Task.class);

    protected String _groupName;
    protected String _accessKey;
    protected String _accessSecret;

    public void setGroupName(String name) {
        _groupName = name;
    }

    public String getName() {
        return _groupName;
    }

    public String getAccessKey() {
        return _accessKey;
    }

    public void setAccessKey(String accessKey) {
        _accessKey = accessKey;
    }

    public String getAccessSecret() {
        return _accessSecret;
    }

    public void setAccessSecret(String accessSecret) {
        _accessSecret = accessSecret;
    }
}
