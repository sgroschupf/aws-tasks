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
package datameer.awstasks.ant.ec2.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;

import datameer.awstasks.aws.ec2.ssh.SshClient;
import datameer.awstasks.util.IoUtil;

public class SshExec extends SshCommand {

    private static final Logger LOG = Logger.getLogger(SshExec.class);

    private String _command;
    private File _commandFile;
    private String _outputProperty;

    public String getCommand() {
        return _command;
    }

    public void setCommand(String command) {
        _command = command;
    }

    public File getCommandFile() {
        return _commandFile;
    }

    public void setCommandFile(File commandFile) {
        _commandFile = commandFile;
    }

    public void setOutputProperty(String outputProperty) {
        _outputProperty = outputProperty;
    }

    public String getOutputProperty() {
        return _outputProperty;
    }

    @Override
    public String toString() {
        return _command;
    }

    @Override
    public void execute(Project project, Map<String, String> propertyMap, SshClient sshClient) throws IOException {
        execute(project, propertyMap, sshClient, null);
    }

    @Override
    public void execute(Project project, Map<String, String> propertyMap, SshClient sshClient, int[] targetInstances) throws IOException {
        OutputStream outputStream = IoUtil.closeProtectedStream(System.out);
        boolean pipeResultToProperty = getOutputProperty() != null;
        if (pipeResultToProperty) {
            outputStream = new ByteArrayOutputStream();
        }
        if (getCommandFile() == null) {
            substituteVariables(propertyMap);
            if (targetInstances == null) {
                sshClient.executeCommand(getCommand(), outputStream);
            } else {
                sshClient.executeCommand(getCommand(), outputStream, targetInstances);
            }
        } else {
            if (targetInstances == null) {
                sshClient.executeCommandFile(getCommandFile(), outputStream);
            } else {
                sshClient.executeCommandFile(getCommandFile(), outputStream, targetInstances);
            }
        }
        if (pipeResultToProperty) {
            String result = new String(((ByteArrayOutputStream) outputStream).toByteArray());
            propertyMap.put(getOutputProperty(), result);
            project.setProperty(getOutputProperty(), result);
        }

    }

    private void substituteVariables(Map<String, String> propertyMap) {
        String command = getCommand();
        if (command.contains("$")) {
            for (String propertyNam : propertyMap.keySet()) {
                try {
                    command = command.replaceAll("\\$" + propertyNam, propertyMap.get(propertyNam));
                    command = command.replaceAll("\\$\\{" + propertyNam + "\\}", propertyMap.get(propertyNam));
                } catch (Exception e) {
                    throw new RuntimeException("failed to replace '" + propertyNam + "=" + propertyMap.get(propertyNam) + "'", e);
                }
            }
        }
        if (!command.equals(getCommand())) {
            LOG.debug("substitute '" + getCommand() + "' with '" + command + "'");
            setCommand(command);
        }
    }

}
