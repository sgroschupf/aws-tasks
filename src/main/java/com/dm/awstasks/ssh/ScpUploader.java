package com.dm.awstasks.ssh;

import java.io.File;

public interface ScpUploader {

    void uploadFile(File localFile, String targetPath);

    void downloadFile(String remoteFile, File localPath);
}
