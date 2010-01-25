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
package datameer.awstasks.ssh;

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
