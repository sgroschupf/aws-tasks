package com.dm.awstasks.ec2.ant.model;

public class ScpDownload extends ScpUpload {

    private boolean _recursiv;

    public boolean isRecursiv() {
        return _recursiv;
    }

    public void setRecursiv(boolean recursiv) {
        _recursiv = recursiv;
    }
}
