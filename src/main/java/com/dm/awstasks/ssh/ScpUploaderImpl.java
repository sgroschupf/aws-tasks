package com.dm.awstasks.ssh;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;

public class ScpUploaderImpl implements ScpUploader {

    private static final Logger LOG = Logger.getLogger(ScpUploaderImpl.class);

    private final File _privateKey;
    private final String _username;
    private final List<String> _hostnames;

    public ScpUploaderImpl(File privateKey, List<String> hostnames, String username) {
        _privateKey = privateKey;
        _hostnames = hostnames;
        _username = username;
    }

    public void uploadFile(File localFile, String targetPath) {
        for (String host : _hostnames) {
            String remotePath = constructRemotePath(host, targetPath);
            Scp scp = createScp(host);
            scp.setLocalFile(localFile.getAbsolutePath());
            scp.setRemoteTodir(remotePath);

            LOG.info(String.format("uploading file '%s' to '%s'", localFile.getAbsolutePath(), remotePath));
            scp.execute();
        }
    }

    public void downloadFile(String remoteFile, File localPath) {
        for (String host : _hostnames) {
            String remotePath = constructRemotePath(host, remoteFile);
            Scp scp = createScp(host);
            scp.setRemoteFile(remotePath);
            scp.setLocalTodir(localPath.getAbsolutePath());

            LOG.info(String.format("downloading file '%s' to '%s'", remotePath, localPath.getAbsolutePath()));
            scp.execute();
        }
    }

    private Scp createScp(String host) {
        Scp scp = new Scp();
        Project project = new Project();
        scp.setProject(project);
        scp.setUsername(_username);
        scp.setKeyfile(_privateKey.getAbsolutePath());
        scp.setTrust(true);
        scp.setHost(host);
        scp.setVerbose(true);
        scp.setFailonerror(true);
        return scp;
    }

    private String constructRemotePath(String host, String filePath) {
        return _username + ":" + "@" + host + ":" + filePath;
    }
}
