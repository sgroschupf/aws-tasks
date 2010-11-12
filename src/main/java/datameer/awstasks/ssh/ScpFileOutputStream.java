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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

import datameer.awstasks.util.SshUtil;

public class ScpFileOutputStream extends OutputStream {

    private final Channel _channel;
    private final OutputStream _outputStream;
    private final InputStream _inputStream;

    public ScpFileOutputStream(Session session, String remoteFile, long length) throws IOException {
        String command = ScpUploadCommand.constructScpUploadCommand(false, remoteFile);
        _channel = SshUtil.openExecChannel(session, command);
        _outputStream = _channel.getOutputStream();
        _inputStream = _channel.getInputStream();
        SshUtil.checkAcknowledgement(_inputStream);
        SshUtil.writeAcknowledgedMessage("C0644 " + length + " " + new File(remoteFile).getName() + "\n", _inputStream, _outputStream);
    }

    @Override
    public void write(int b) throws IOException {
        _outputStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        _outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        _outputStream.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        _outputStream.flush();
        SshUtil.sendAckOk(_outputStream);
        SshUtil.checkAcknowledgement(_inputStream);
        _channel.disconnect();
    }

}
