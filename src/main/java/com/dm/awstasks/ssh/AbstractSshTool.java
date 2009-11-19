package com.dm.awstasks.ssh;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHBase;

public abstract class AbstractSshTool {

    protected static final Logger LOG = Logger.getLogger(AbstractSshTool.class);

    protected final File _privateKey;
    protected final String _username;
    protected final List<String> _hostnames;

    public AbstractSshTool(File privateKey, List<String> hostnames, String username) {
        _privateKey = privateKey;
        _hostnames = hostnames;
        _username = username;
    }

    protected void configureSshBase(SSHBase sshExec, String host) {
        sshExec.setProject(new Project());
        sshExec.setUsername(_username);
        sshExec.setKeyfile(_privateKey.getAbsolutePath());
        sshExec.setTrust(true);
        sshExec.setHost(host);
        sshExec.setVerbose(true);
        sshExec.setFailonerror(true);
    }
}
