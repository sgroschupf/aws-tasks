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
package datameer.awstasks.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import awstasks.com.jcraft.jsch.Channel;
import awstasks.com.jcraft.jsch.ChannelExec;
import awstasks.com.jcraft.jsch.JSchException;
import awstasks.com.jcraft.jsch.Session;

public class SshUtil {

    public final static Channel openExecChannel(Session session, String command) throws IOException {
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.connect();
            return channel;
        } catch (JSchException e) {
            throw new IOException("could not open exec channel with command " + command, e);
        }
    }

    /**
     * Reads the acknowledge byte and throws a IOException if the response indicates an error.
     */
    public final static void checkAcknowledgement(InputStream in) throws IOException {
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

    public final static void sendAckOk(OutputStream out) throws IOException {
        out.write(new byte[] { 0 });
        out.flush();
    }

    public final static void writeAcknowledgedMessage(String message, InputStream in, OutputStream out) throws IOException {
        out.write((message).getBytes());
        out.flush();
        checkAcknowledgement(in);
    }

}
