package com.dm.awstasks.ec2.ant.model;

import java.io.File;

public class ScpUpload extends SshCommand {

    private File _localFile;
    private String _remotePath;

    public File getLocalFile() {
        return _localFile;
    }

    public void setLocalFile(File localFile) {
        _localFile = localFile;
    }

    public String getRemotePath() {
        return _remotePath;
    }

    public void setRemotePath(String remotePath) {
        _remotePath = remotePath;
    }

}
