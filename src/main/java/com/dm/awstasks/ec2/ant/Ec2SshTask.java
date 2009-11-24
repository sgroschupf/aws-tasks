package com.dm.awstasks.ec2.ant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;

import com.dm.awstasks.ec2.InstanceGroup;
import com.dm.awstasks.ec2.InstanceGroupImpl;
import com.dm.awstasks.ec2.ant.model.ScpDownload;
import com.dm.awstasks.ec2.ant.model.ScpUpload;
import com.dm.awstasks.ec2.ant.model.SshCommand;
import com.dm.awstasks.ec2.ant.model.SshExec;
import com.dm.awstasks.ec2.ssh.SshClient;
import com.xerox.amazonws.ec2.Jec2;

public class Ec2SshTask extends AbstractEc2SshTask {

    private List<SshCommand> _sshCommands = new ArrayList<SshCommand>();

    public void addUpload(ScpUpload upload) {
        _sshCommands.add(upload);
    }

    public void addDownload(ScpDownload download) {
        _sshCommands.add(download);
    }

    public void addExec(SshExec sshExec) {
        _sshCommands.add(sshExec);
    }

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName() + " for group '" + _groupName + "'");
        Jec2 ec2 = new Jec2(_accessKey, _accessSecret);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);

        try {
            instanceGroup.connectTo(_groupName);
            int instanceCount = instanceGroup.instanceCount();
            // verify targetIndexes specifications
            for (SshCommand sshCommand : _sshCommands) {
                if (!sshCommand.isToAllInstances()) {
                    sshCommand.compileTargetInstances(instanceCount);
                }
            }

            SshClient sshClient = instanceGroup.createSshClient(_username, _keyFile);
            for (SshCommand sshCommand : _sshCommands) {
                if (sshCommand instanceof SshExec) {
                    doSshExec(sshClient, (SshExec) sshCommand, instanceCount);
                } else if (sshCommand instanceof ScpUpload) {
                    doUpload(sshClient, (ScpUpload) sshCommand, instanceCount);
                } else if (sshCommand instanceof ScpDownload) {
                    doDownload(sshClient, (ScpDownload) sshCommand, instanceCount);
                } else {
                    throw new IllegalStateException("type '" + sshCommand.getClass().getName() + "' not supported here");
                }
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private void doSshExec(SshClient sshClient, SshExec sshCommand, int instanceCount) throws IOException {
        if (sshCommand.getCommandFile() == null) {
            if (sshCommand.isToAllInstances()) {
                sshClient.executeCommand(sshCommand.getCommand());
            } else {
                sshClient.executeCommand(sshCommand.getCommand(), sshCommand.compileTargetInstances(instanceCount));
            }
        } else {
            if (sshCommand.isToAllInstances()) {
                sshClient.executeCommandFile(sshCommand.getCommandFile());
            } else {
                sshClient.executeCommandFile(sshCommand.getCommandFile(), sshCommand.compileTargetInstances(instanceCount));
            }
        }
    }

    private void doUpload(SshClient scpUploader, ScpUpload upload, int instanceCount) throws IOException {
        if (upload.isToAllInstances()) {
            scpUploader.uploadFile(upload.getLocalFile(), upload.getRemotePath());
        } else {
            scpUploader.uploadFile(upload.getLocalFile(), upload.getRemotePath(), upload.compileTargetInstances(instanceCount));
        }
    }

    private void doDownload(SshClient scpUploader, ScpDownload download, int instanceCount) throws IOException {
        if (download.isToAllInstances()) {
            scpUploader.downloadFile(download.getRemotePath(), download.getLocalFile(), download.isRecursiv());
        } else {
            scpUploader.downloadFile(download.getRemotePath(), download.getLocalFile(), download.isRecursiv(), download.compileTargetInstances(instanceCount));
        }
    }

}
