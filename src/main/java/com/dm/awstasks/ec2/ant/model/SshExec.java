package com.dm.awstasks.ec2.ant.model;

import java.io.File;

public class SshExec extends SshCommand {

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

}
