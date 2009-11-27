package com.dm.awstasks.ec2.ssh;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface SshClient {

    void executeCommand(String command, OutputStream outputStream) throws IOException;

    void executeCommand(String command, OutputStream outputStream, int[] targetedInstances) throws IOException;

    void executeCommandFile(File commandFile, OutputStream outputStream) throws IOException;

    void executeCommandFile(File commandFile, OutputStream outputStream, int[] targetedInstances) throws IOException;

    void uploadFile(File localFile, String targetPath) throws IOException;

    void uploadFile(File localFile, String targetPath, int[] targetedInstances) throws IOException;

    void downloadFile(String remoteFile, File localPath, boolean recursiv) throws IOException;

    void downloadFile(String remoteFile, File localPath, boolean recursiv, int[] targetedInstances) throws IOException;
}
