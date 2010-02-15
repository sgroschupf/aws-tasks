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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

import datameer.awstasks.util.IoUtil;

public class ScpDownloadCommand extends JschCommand {

    private static final byte LINE_FEED = 0x0a;
    private static final String SCP_DOWNLOAD_COMMAND = "scp -f ";

    private final String _remoteFile;
    private final File _localFile;
    private final boolean _recursive;

    public ScpDownloadCommand(String remoteFile, File localFile, boolean recursive) {
        _remoteFile = remoteFile;
        _localFile = localFile;
        _recursive = recursive;
    }

    @Override
    public void execute(Session session) throws IOException {
        String command = constructScpInitCommand(_remoteFile, _recursive);
        Channel channel = openExecChannel(session, command);
        try {
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            sendAckOk(out);
            download(in, out, _localFile);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    protected final static String constructScpInitCommand(String remoteFile, boolean recursive) {
        String command = SCP_DOWNLOAD_COMMAND;
        if (recursive) {
            command += "-r ";
        }
        command += "'" + remoteFile + "'";
        return command;
    }

    private final static void download(InputStream in, OutputStream out, File localFile) throws IOException {
        File startFile = localFile;
        while (true) {
            // C0644 filesize filename - header for a regular file
            // T time 0 time 0\n - present if perserve time.
            // D directory - this is the header for a directory.
            String serverResponse = readServerResponse(in);
            if (serverResponse == null) {
                return;
            }
            if (serverResponse.charAt(0) == 'C') {
                parseAndDownloadFile(serverResponse, startFile, out, in);
            } else if (serverResponse.charAt(0) == 'D') {
                startFile = parseAndCreateDirectory(serverResponse, startFile);
                sendAckOk(out);
            } else if (serverResponse.charAt(0) == 'E') {
                startFile = startFile.getParentFile();
                sendAckOk(out);
            } else if (serverResponse.charAt(0) == '\01' || serverResponse.charAt(0) == '\02') {
                // this indicates an error.
                throw new IOException(serverResponse.substring(1));
            }
        }
    }

    protected static String readServerResponse(InputStream in) throws IOException, UnsupportedEncodingException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        while (true) {
            int read = in.read();
            if (read < 0) {
                return null;
            }
            if ((byte) read == LINE_FEED) {
                break;
            }
            stream.write(read);
        }
        String serverResponse = stream.toString("UTF-8");
        return serverResponse;
    }

    private final static File parseAndCreateDirectory(String serverResponse, File localFile) {
        int start = serverResponse.indexOf(" ");
        // appears that the next token is not used and it's zero.
        start = serverResponse.indexOf(" ", start + 1);
        String directoryName = serverResponse.substring(start + 1);
        if (localFile.isDirectory()) {
            File dir = new File(localFile, directoryName);
            dir.mkdir();
            LOG.info("Creating: " + dir);
            return dir;
        }
        return null;
    }

    private final static void parseAndDownloadFile(String serverResponse, File localFile, OutputStream out, InputStream in) throws IOException {
        int start = 0;
        int end = serverResponse.indexOf(" ", start + 1);
        start = end + 1;
        end = serverResponse.indexOf(" ", start + 1);
        long filesize = Long.parseLong(serverResponse.substring(start, end));
        String filename = serverResponse.substring(end + 1);
        LOG.info("Receiving: " + filename + " : " + filesize);
        File transferFile = (localFile.isDirectory()) ? new File(localFile, filename) : localFile;
        downloadFile(transferFile, filesize, out, in);
        checkAcknowledgement(in);
        sendAckOk(out);
    }

    private final static void downloadFile(File localFile, long filesize, OutputStream out, InputStream in) throws IOException {
        sendAckOk(out);

        // read a content of lfile
        FileOutputStream fos = new FileOutputStream(localFile);
        long totalLength = 0;
        long startTime = System.currentTimeMillis();

        try {
            IoUtil.copyBytes(in, fos, filesize);
        } finally {
            long endTime = System.currentTimeMillis();
            logStats(startTime, endTime, totalLength);
            fos.flush();
            fos.close();
        }
    }

}
