package com.dm.awstasks.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public abstract class JschCommand {

    protected static final Logger LOG = Logger.getLogger(JschCommand.class);

    public abstract void execute(Session session) throws IOException;

    protected final static Channel openExecChannel(Session session, String command) throws IOException {
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.connect();
            return channel;
        } catch (JSchException e) {
            throw new IOException("could not open exec channel with command " + command, e);
        }
    }

    protected final static void sendAckOk(OutputStream out) throws IOException {
        out.write(new byte[] { 0 });
        out.flush();
    }

    /**
     * Reads the acknowledge byte and throws a IOException if the response indicates an error.
     */
    protected final static void checkAcknowledgement(InputStream in) throws IOException {
        int ackByte = in.read();
        // 0 for success
        // 1 for error
        // 2 for fatal error

        if (ackByte == -1) {
            // didn't receive any response
            throw new IOException("No response from server");
        } else if (ackByte != 0) {
            StringBuilder sb = new StringBuilder();
            int c = in.read();
            while (c > 0 && c != '\n') {
                sb.append((char) c);
                c = in.read();
            }

            if (ackByte == 1) {
                throw new IOException("server indicated an error: " + sb.toString());
            } else if (ackByte == 2) {
                throw new IOException("server indicated a fatal error: " + sb.toString());
            } else {
                throw new IOException("unknown response, code " + ackByte + " message: " + sb.toString());
            }
        }
    }

    protected final static void logStats(long timeStarted, long timeEnded, long totalLength) {
        double durationInSec = (timeEnded - timeStarted) / 1000.0;
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(1);
        LOG.debug("File transfer time: " + format.format(durationInSec) + " Average Rate: " + format.format(totalLength / durationInSec) + " B/s");
    }
}
