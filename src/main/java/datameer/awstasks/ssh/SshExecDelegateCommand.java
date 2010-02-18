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

import java.io.IOException;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

import datameer.awstasks.exec.ExecOutputHandler;
import datameer.awstasks.exec.ShellCommand;

public class SshExecDelegateCommand<R> extends JschCommand {

    private final ShellCommand<?> _command;
    private final ExecOutputHandler<R> _outputHandler;
    private R _result;

    public SshExecDelegateCommand(ShellCommand<?> command, ExecOutputHandler<R> outputHandler) {
        _command = command;
        _outputHandler = outputHandler;
    }

    @Override
    public void execute(Session session) throws IOException {
        StringBuilder builder = new StringBuilder();
        String[] commands = _command.getCommand();
        for (String command : commands) {
            builder.append(command);
            builder.append(' ');
        }
        executeCommand(session, builder.toString());
    }

    private void executeCommand(Session session, String command) throws IOException {
        final Channel channel = openExecChannel(session, command);
        ToLineOutputStream outputStream = new ToLineOutputStream(_outputHandler);
        // OutputStream outputStream = IoUtil.closeProtectedStream(System.out);
        channel.setOutputStream(outputStream);
        channel.setExtOutputStream(outputStream);
        try {
            do {
                Thread.sleep(250);
            } while (!channel.isClosed());// jz: should we also build in a timeout mechanism ?
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        int exitCode = channel.getExitStatus();
        if (exitCode != 0 && _command.failOnError()) {
            throw new IOException("could not execute command '" + command + "', got exit code " + exitCode);
        }
        _result = _outputHandler.getResult(exitCode);
    }

    public R getResult() {
        return _result;
    }

    static class ToLineOutputStream extends OutputStream {

        private final ExecOutputHandler<?> _outputHandler;

        public ToLineOutputStream(ExecOutputHandler<?> outputHandler) {
            _outputHandler = outputHandler;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            fireText(b, off, len);
        }

        private void fireText(byte[] b, int off, int len) {
            // System.err.println("'" + new String(b, off, len - 1) + "'");
            int lastStart = off;
            for (int i = off; i < len; i++) {
                char c = (char) b[i];
                if (c == '\n' || c == '\r') {
                    fireLine(b, lastStart, i - lastStart);
                    lastStart = i + 1;
                }
            }
            if (lastStart < len) {
                int lenOfLast = len - lastStart + off;
                if (b[lastStart + lenOfLast - 1] == '\n') {
                    lenOfLast--;
                }
                fireLine(b, lastStart, lenOfLast);
            }
        }

        private void fireLine(byte[] b, int off, int len) {
            // System.err.println(new String(b, off, len));
            _outputHandler.handleLine(new String(b, off, len));
        }

        @Override
        public void write(byte[] b) throws IOException {
            fireText(b, 0, b.length);
        }

        @Override
        public void write(int b) throws IOException {
            throw new UnsupportedOperationException();
        }

    }

}
