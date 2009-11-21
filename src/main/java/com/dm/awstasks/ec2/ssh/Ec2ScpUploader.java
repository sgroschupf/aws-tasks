package com.dm.awstasks.ec2.ssh;

import java.io.File;
import java.io.IOException;

public interface Ec2ScpUploader {

    void uploadFile(File localFile, String targetPath) throws IOException;

    void uploadFile(File localFile, String targetPath, int[] targetedInstances) throws IOException;

    void downloadFile(String remoteFile, File localPath, boolean recursiv) throws IOException;

    void downloadFile(String remoteFile, File localPath, boolean recursiv, int[] targetedInstances) throws IOException;

}
