package com.dm.awstasks.ec2.ssh;

import java.io.File;
import java.io.IOException;

public interface SshClient {

    void executeCommand(String command) throws IOException;

    void executeCommand(String command, int[] targetedInstances) throws IOException;

    void executeCommandFile(File commandFile) throws IOException;

    void executeCommandFile(File commandFile, int[] targetedInstances) throws IOException;

    void uploadFile(File localFile, String targetPath) throws IOException;

    void uploadFile(File localFile, String targetPath, int[] targetedInstances) throws IOException;

    void downloadFile(String remoteFile, File localPath, boolean recursiv) throws IOException;

    void downloadFile(String remoteFile, File localPath, boolean recursiv, int[] targetedInstances) throws IOException;
}
