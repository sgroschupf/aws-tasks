/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datameer.awstasks.ant.ec2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

import com.amazonaws.services.ec2.AmazonEC2;

import datameer.awstasks.ant.ec2.model.ScpDownload;
import datameer.awstasks.ant.ec2.model.ScpUpload;
import datameer.awstasks.ant.ec2.model.SshCommand;
import datameer.awstasks.ant.ec2.model.SshExec;
import datameer.awstasks.aws.ec2.InstanceGroup;
import datameer.awstasks.aws.ec2.InstanceGroupImpl;
import datameer.awstasks.aws.ec2.ssh.SshClient;
import datameer.awstasks.util.IoUtil;

public class Ec2SshTask extends AbstractEc2Task implements TaskContainer {

    private String _username;
    private String _password;
    private File _keyFile;
    private List<Object> _commands = new ArrayList<Object>();
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

    public String getPassword() {
        return _password;
    }

    public void setPassword(String password) {
        _password = password;
    }

    public File getKeyFile() {
        return _keyFile;
    }

    public void setKeyFile(File keyFile) {
        _keyFile = keyFile;
    }

    @Override
    public void addTask(Task task) {
        _commands.add(task);
    }

    public void addUpload(ScpUpload upload) {
        _commands.add(upload);
    }

    public void addDownload(ScpDownload download) {
        _commands.add(download);
    }

    public void addExec(SshExec sshExec) {
        _commands.add(sshExec);
    }

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName() + " for group '" + _groupName + "'");
        if (_instanceGroup == null) {
            AmazonEC2 ec2 = createEc2();
            _instanceGroup = new InstanceGroupImpl(ec2);
        }

        try {
            _instanceGroup.connectTo(_groupName);
            int instanceCount = _instanceGroup.instanceCount();

            // verify targetIndexes specifications
            for (Object command : _commands) {
                if (command instanceof SshCommand) {
                    SshCommand sshCommand = (SshCommand) command;
                    if (!sshCommand.isToAllInstances()) {
                        sshCommand.compileTargetInstances(instanceCount);
                    }
                }
            }

            // execute the commands
            SshClient sshClient = createSshClient();
            for (Object command : _commands) {
                if (isCommandEnabled(command)) {
                    if (command instanceof SshExec) {
                        doSshExec(sshClient, (SshExec) command, instanceCount);
                    } else if (command instanceof ScpDownload) {
                        doDownload(sshClient, (ScpDownload) command, instanceCount);
                    } else if (command instanceof ScpUpload) {
                        doUpload(sshClient, (ScpUpload) command, instanceCount);
                    } else if (command instanceof Task) {
                        ((Task) command).perform();
                    } else {
                        throw new IllegalStateException("type '" + command.getClass().getName() + "' not supported here");
                    }
                } else {
                    System.out.println("skipping command '" + command + "'");
                }
            }

            for (String propertyName : _propertyMap.keySet()) {
                getProject().setNewProperty(propertyName, _propertyMap.get(propertyName));
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private SshClient createSshClient() {
        if (_keyFile != null) {
            return _instanceGroup.createSshClient(_username, _keyFile);
        }
        return _instanceGroup.createSshClient(_username, _password);
    }

    private boolean isCommandEnabled(Object command) {
        if (!(command instanceof SshCommand)) {
            return true;
        }
        return ((SshCommand) command).isIfFulfilled(getProject());
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
            getProject().setProperty(sshCommand.getOutputProperty(), result);
        }
    }

    private void substituteVariables(SshExec sshCommand) {
        String command = sshCommand.getCommand();
        if (command.contains("$")) {
            for (String propertyNam : _propertyMap.keySet()) {
                try {
                    command = command.replaceAll("\\$" + propertyNam, _propertyMap.get(propertyNam));
                    command = command.replaceAll("\\$\\{" + propertyNam + "\\}", _propertyMap.get(propertyNam));
                } catch (Exception e) {
                    throw new RuntimeException("failed to replace '" + propertyNam + "=" + _propertyMap.get(propertyNam) + "'", e);
                }
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
