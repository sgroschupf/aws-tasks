package com.dm.awstasks.ssh;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.taskdefs.optional.ssh.SSHExec;

public class SshExecutorImpl extends AbstractSshTool implements SshExecutor {

    public SshExecutorImpl(File privateKey, List<String> hostnames, String username) {
        super(privateKey, hostnames, username);
    }

    @Override
    public void executeCommand(String command) {
        for (String host : _hostnames) {
            SSHExec sshExec = createSshExec(host);
            sshExec.setCommand(command);
            sshExec.execute();
        }
    }

    @Override
    public void executeCommandFile(File commandFile) {
        for (String host : _hostnames) {
            SSHExec sshExec = createSshExec(host);
            sshExec.setCommandResource(commandFile.getAbsolutePath());
            sshExec.execute();
        }
    }

    private SSHExec createSshExec(String host) {
        SSHExec sshExec = new SSHExec();
        configureSshBase(sshExec, host);
        return sshExec;
    }

}
