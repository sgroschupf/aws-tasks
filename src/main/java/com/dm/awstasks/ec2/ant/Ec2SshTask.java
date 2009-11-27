package com.dm.awstasks.ec2.ant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;

import com.dm.awstasks.ec2.InstanceGroup;
import com.dm.awstasks.ec2.InstanceGroupImpl;
import com.dm.awstasks.ec2.ant.model.ScpDownload;
import com.dm.awstasks.ec2.ant.model.ScpUpload;
import com.dm.awstasks.ec2.ant.model.SshCommand;
import com.dm.awstasks.ec2.ant.model.SshExec;
import com.dm.awstasks.ec2.ssh.SshClient;
import com.dm.awstasks.util.IoUtil;
import com.xerox.amazonws.ec2.Jec2;

public class Ec2SshTask extends AbstractEc2Task {

    private String _username;
    private File _keyFile;
    private List<SshCommand> _sshCommands = new ArrayList<SshCommand>();
    private Map<String, String> _propertyMap = new HashMap<String, String>();
    private InstanceGroup _instanceGroup;

    public Ec2SshTask() {
        // default constructor - needed by ant
    }

    public Ec2SshTask(InstanceGroup instanceGroup) {
        _instanceGroup = instanceGroup;
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(String username) {
        _username = username;
    }

    public File getKeyFile() {
        return _keyFile;
    }

    public void setKeyFile(File keyFile) {
        _keyFile = keyFile;
    }

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
        if (_instanceGroup == null) {
            Jec2 ec2 = new Jec2(_accessKey, _accessSecret);
            _instanceGroup = new InstanceGroupImpl(ec2);
        }

        try {
            _instanceGroup.connectTo(_groupName);
            int instanceCount = _instanceGroup.instanceCount();
            // verify targetIndexes specifications
            for (SshCommand sshCommand : _sshCommands) {
                if (!sshCommand.isToAllInstances()) {
                    sshCommand.compileTargetInstances(instanceCount);
                }
            }

            // execute the commands
            SshClient sshClient = _instanceGroup.createSshClient(_username, _keyFile);
            for (SshCommand sshCommand : _sshCommands) {
                if (sshCommand instanceof SshExec) {
                    doSshExec(sshClient, (SshExec) sshCommand, instanceCount);
                } else if (sshCommand instanceof ScpDownload) {
                    doDownload(sshClient, (ScpDownload) sshCommand, instanceCount);
                } else if (sshCommand instanceof ScpUpload) {
                    doUpload(sshClient, (ScpUpload) sshCommand, instanceCount);
                } else {
                    throw new IllegalStateException("type '" + sshCommand.getClass().getName() + "' not supported here");
                }
            }

            for (String propertyName : _propertyMap.keySet()) {
                getProject().setNewProperty(propertyName, _propertyMap.get(propertyName));
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private void doSshExec(SshClient sshClient, SshExec sshCommand, int instanceCount) throws IOException {
        OutputStream outputStream = IoUtil.closeProtectedStream(System.out);
        boolean pipeResultToProperty = sshCommand.getOutputProperty() != null;
        if (pipeResultToProperty) {
            outputStream = new ByteArrayOutputStream();
        }
        if (sshCommand.getCommandFile() == null) {
            substituteVariables(sshCommand);
            if (sshCommand.isToAllInstances()) {
                sshClient.executeCommand(sshCommand.getCommand(), outputStream);
            } else {
                sshClient.executeCommand(sshCommand.getCommand(), outputStream, sshCommand.compileTargetInstances(instanceCount));
            }
        } else {
            if (sshCommand.isToAllInstances()) {
                sshClient.executeCommandFile(sshCommand.getCommandFile(), outputStream);
            } else {
                sshClient.executeCommandFile(sshCommand.getCommandFile(), outputStream, sshCommand.compileTargetInstances(instanceCount));
            }
        }
        if (pipeResultToProperty) {
            String result = new String(((ByteArrayOutputStream) outputStream).toByteArray());
            _propertyMap.put(sshCommand.getOutputProperty(), result);
        }
    }

    private void substituteVariables(SshExec sshCommand) {
        String command = sshCommand.getCommand();
        if (command.contains("$")) {
            for (String propertyNam : _propertyMap.keySet()) {
                command = command.replaceAll("\\$" + propertyNam, _propertyMap.get(propertyNam));
                command = command.replaceAll("\\$\\{" + propertyNam + "\\}", _propertyMap.get(propertyNam));
            }
        }
        if (!command.equals(sshCommand.getCommand())) {
            LOG.debug("substitute '" + sshCommand.getCommand() + "' with '" + command + "'");
            sshCommand.setCommand(command);
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
