package com.dm.awstasks.ec2.ant;

import java.io.File;

public class AbstractEc2SshTask extends AbstractEc2Task {

    protected String _username;
    protected File _keyFile;

    public String getUsername() {
        return _username;
    }

    public void setUsername(String username) {
        _username = username;
    }

    public File getKeyFile() {
        return _keyFile;
    }

    public void setKeyFile(File keyFile) {
        _keyFile = keyFile;
    }

}
