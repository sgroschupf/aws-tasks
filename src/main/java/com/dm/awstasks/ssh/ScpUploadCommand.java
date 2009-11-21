package com.dm.awstasks.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.dm.awstasks.util.IoUtil;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

public class ScpUploadCommand extends JschCommand {

    private static final String SCP_UPLOAD_FOLDER_COMMAND = "scp -r -d -t ";
    private static final String SCP_UPLOAD_FILE_COMMAND = "scp -t ";

    private final File _localFile;
    private final String _targetPath;

    public ScpUploadCommand(File localFile, String targetPath) {
        _localFile = localFile;
        _targetPath = targetPath;
    }

    @Override
    public void execute(Session session) throws IOException {
        String command = constructScpUploadCommand(_localFile, _targetPath);
        Channel channel = openExecChannel(session, command);
        try {
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            checkAcknowledgement(in);
            if (_localFile.isDirectory()) {
                uploadFolder(_localFile, in, out);
            } else {
                uploadFile(_localFile, in, out);
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private final static String constructScpUploadCommand(File localFile, String remotePath) {
        if (localFile.isDirectory()) {
            return SCP_UPLOAD_FOLDER_COMMAND + remotePath;
        }
        return SCP_UPLOAD_FILE_COMMAND + remotePath;
    }

    private static void uploadFolder(File folder, InputStream in, OutputStream out) throws IOException {
        writeAcknowledgedMessage("D0755 0 " + folder.getName() + "\n", in, out);

        uploadFolderChildren(folder, in, out);
        writeAcknowledgedMessage("E\n", in, out);
    }

    private static void uploadFolderChildren(File localFile, InputStream in, OutputStream out) throws IOException {
        File[] files = localFile.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                uploadFolder(file, in, out);
            } else {
                uploadFile(file, in, out);
            }
        }
    }

    private static void uploadFile(File localFile, InputStream in, OutputStream out) throws IOException {
        writeAcknowledgedMessage("C0644 " + localFile.length() + " " + localFile.getName() + "\n", in, out);
        FileInputStream fileInputStream = new FileInputStream(localFile);
        long startTime = System.currentTimeMillis();
        long totalLength = 0;

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending: " + localFile.getName() + " : " + localFile.length() + " bytes");
            }
            totalLength = IoUtil.copyBytes(fileInputStream, out);
            out.flush();
            sendAckOk(out);
            checkAcknowledgement(in);
        } finally {
            if (LOG.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                logStats(startTime, endTime, totalLength);
            }
            fileInputStream.close();
        }
    }

    private final static void writeAcknowledgedMessage(String message, InputStream in, OutputStream out) throws IOException {
        out.write((message).getBytes());
        out.flush();
        checkAcknowledgement(in);
    }

}
