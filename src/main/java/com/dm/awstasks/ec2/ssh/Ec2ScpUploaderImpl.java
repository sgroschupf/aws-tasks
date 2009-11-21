package com.dm.awstasks.ec2.ssh;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.dm.awstasks.ssh.JschRunner;
import com.dm.awstasks.ssh.ScpDownloadCommand;
import com.dm.awstasks.ssh.ScpUploadCommand;

public class Ec2ScpUploaderImpl extends AbstractEc2SshTool implements Ec2ScpUploader {

    public Ec2ScpUploaderImpl(File privateKey, List<String> hostnames, String username) {
        super(privateKey, hostnames, username);
    }

    public void uploadFile(File localFile, String targetPath) throws IOException {
        uploadFile(_hostnames, localFile, targetPath);
    }

    @Override
    public void uploadFile(File localFile, String targetPath, int[] instanceIndex) throws IOException {
        List<String> hostnames = getHosts(instanceIndex);
        uploadFile(hostnames, localFile, targetPath);
    }

    private void uploadFile(List<String> hosts, File localFile, String targetPath) throws IOException {
        for (String host : hosts) {
            LOG.info(String.format("uploading file '%s' to '%s'", localFile.getAbsolutePath(), constructRemotePath(host, targetPath)));
            JschRunner jschRunner = createJschRunner(host);
            jschRunner.run(new ScpUploadCommand(localFile, targetPath));
        }
    }

    public void downloadFile(String remoteFile, File localPath, boolean recursiv) throws IOException {
        downloadFiles(_hostnames, remoteFile, localPath, recursiv);
    }

    @Override
    public void downloadFile(String remoteFile, File localPath, boolean recursiv, int[] instanceIndex) throws IOException {
        List<String> hosts = getHosts(instanceIndex);
        downloadFiles(hosts, remoteFile, localPath, recursiv);
    }

    private void downloadFiles(List<String> hostnames, String remoteFile, File localPath, boolean recursiv) throws IOException {
        for (String host : hostnames) {
            LOG.info(String.format("downloading file '%s' to '%s'", constructRemotePath(host, remoteFile), localPath.getAbsolutePath()));
            JschRunner jschRunner = createJschRunner(host);
            jschRunner.run(new ScpDownloadCommand(remoteFile, localPath, recursiv));
        }
    }

    private String constructRemotePath(String host, String filePath) {
        return _username + ":" + "@" + host + ":" + filePath;
    }

}
