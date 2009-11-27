package com.dm.awstasks.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

public class SshExecCommand extends JschCommand {

    private final String _command;
    private final File _commandFile;
    private final OutputStream _outputStream;

    public SshExecCommand(String command, OutputStream outputStream) {
        _command = command;
        _outputStream = outputStream;
        _commandFile = null;
    }

    public SshExecCommand(File commandFile, OutputStream outputStream) {
        _commandFile = commandFile;
        _outputStream = outputStream;
        _command = null;
    }

    @Override
    public void execute(Session session) throws IOException {
        if (_command != null) {
            executeCommand(session, _command);
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(_commandFile)));
            String command;
            while ((command = reader.readLine()) != null) {
                executeCommand(session, command);
            }
            reader.close();
        }
    }

    private void executeCommand(Session session, String command) throws IOException {
        final Channel channel = openExecChannel(session, command);
        channel.setOutputStream(_outputStream);
        channel.setExtOutputStream(_outputStream);

        try {
            do {
                Thread.sleep(500);
            } while (!channel.isClosed());// jz: should we also build in a timeout mechanism ?
        } catch (InterruptedException e) {
            Thread.interrupted();
        }

        int exitCode = channel.getExitStatus();
        if (exitCode != 0) {
            String msg = "Remote command failed with exit status " + exitCode;
            throw new IOException(msg);
        }
    }

}
