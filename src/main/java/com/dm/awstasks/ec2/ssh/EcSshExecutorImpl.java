package com.dm.awstasks.ec2.ssh;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.taskdefs.optional.ssh.SSHExec;

public class EcSshExecutorImpl extends AbstractEc2SshTool implements Ec2SshExecutor {

    public EcSshExecutorImpl(File privateKey, List<String> hostnames, String username) {
        super(privateKey, hostnames, username);
    }

    @Override
    public void executeCommand(String command) {
        executeCommand(_hostnames, command);
    }

    @Override
    public void executeCommand(String command, int[] targetedInstances) {
        executeCommand(getHosts(targetedInstances), command);
    }

    private void executeCommand(List<String> hostnames, String command) {
        for (String host : hostnames) {
            SSHExec sshExec = createSshExec(host);
            sshExec.setCommand(command);
            sshExec.execute();
        }
    }

    @Override
    public void executeCommandFile(File commandFile) {
        executeCommandFile(_hostnames, commandFile);
    }

    @Override
    public void executeCommandFile(File commandFile, int[] targetedInstances) {
        executeCommandFile(getHosts(targetedInstances), commandFile);
    }

    private void executeCommandFile(List<String> hostnames, File commandFile) {
        for (String host : hostnames) {
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

    protected void configureSshBase(org.apache.tools.ant.taskdefs.optional.ssh.SSHBase sshExec, String host) {
        // sshExec.setProject(new Project());
        sshExec.setUsername(_username);
        sshExec.setKeyfile(_privateKey.getAbsolutePath());
        sshExec.setTrust(true);
        sshExec.setHost(host);
        sshExec.setVerbose(true);
        sshExec.setFailonerror(true);
    }

}
