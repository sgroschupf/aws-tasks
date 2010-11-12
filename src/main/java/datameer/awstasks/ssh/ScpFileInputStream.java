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
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

import datameer.awstasks.util.SshUtil;

public class ScpFileInputStream extends InputStream {

    private static final Logger LOG = Logger.getLogger(ScpFileInputStream.class);

    private final Session _session;
    private final Channel _execChannel;
    private final OutputStream _sshOutputStream;
    private final InputStream _sshInputStream;
    private long _availible;

    public ScpFileInputStream(Session session, String remoteFile) throws IOException {
        _session = session;
        String command = ScpDownloadCommand.constructScpInitCommand(remoteFile, false);
        _execChannel = SshUtil.openExecChannel(session, command);
        _sshOutputStream = _execChannel.getOutputStream();
        _sshInputStream = _execChannel.getInputStream();
        SshUtil.sendAckOk(_sshOutputStream);
        String serverResponse = ScpDownloadCommand.readServerResponse(_sshInputStream);
        if (serverResponse.charAt(0) != 'C') {
            throw new IllegalStateException("unexpected server response: " + serverResponse);
        }
        int start = 0;
        int end = serverResponse.indexOf(" ", start + 1);
        start = end + 1;
        end = serverResponse.indexOf(" ", start + 1);
        long filesize = Long.parseLong(serverResponse.substring(start, end));
        String filename = serverResponse.substring(end + 1);
        LOG.info("opening file: " + filename + " | " + filesize);
        SshUtil.sendAckOk(_sshOutputStream);
        _availible = filesize;
    }

    @Override
    public int available() throws IOException {
        return (int) _availible;
    }

    @Override
    public int read() throws IOException {
        checkConnection();
        if (_availible == 0) {
            return -1;
        }
        _availible--;
        return _sshInputStream.read();
    }

    private void checkConnection() {
        if (!_session.isConnected()) {
            throw new IllegalStateException("stream is already closed");
        }
    }

    @Override
    public void close() throws IOException {
        try {
            SshUtil.checkAcknowledgement(_sshInputStream);
            SshUtil.sendAckOk(_sshOutputStream);
        } catch (IOException e) {
            // happens in the middle of read
        }
        _execChannel.disconnect();
        _session.disconnect();
    }

}
