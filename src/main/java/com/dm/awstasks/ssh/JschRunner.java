package com.dm.awstasks.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;
import com.jcraft.jsch.UserInfo;

public class JschRunner {

    protected static final Logger LOG = Logger.getLogger(JschRunner.class);

    protected static final int CONNECT_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(5);

    private final String _user;
    private final String _host;
    private int _port = 22;
    private String _keyFile;
    private String _knownHosts = System.getProperty("user.home") + "/.ssh/known_hosts";
    private boolean _trust;

    public JschRunner(String user, String host) {
        _user = user;
        _host = host;
    }

    public String getHost() {
        return _host;
    }

    public void setKeyfile(String keyfile) {
        _keyFile = keyfile;
    }

    public void setKnownHosts(String knownHosts) {
        _knownHosts = knownHosts;
    }

    public void setTrust(boolean trust) {
        _trust = trust;
    }

    public void setPort(int port) {
        _port = port;
    }

    public int getPort() {
        return _port;
    }

    public void run(JschCommand command) throws IOException {
        try {
            Session session = null;
            try {
                session = openSession();
                command.execute(session);
            } finally {
                if (session != null) {
                    session.disconnect();
                }
            }
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    private Session openSession() throws JSchException {
        JSch jsch = new JSch();
        if (_keyFile != null) {
            jsch.addIdentity(_keyFile);
        }

        if (!_trust && _knownHosts != null) {
            LOG.debug("Using known hosts: " + _knownHosts);
            jsch.setKnownHosts(_knownHosts);
        }

        Session session = jsch.getSession(_user, _host, _port);
        session.setSocketFactory(new SocketFactoryWithConnectTimeout());
        session.setUserInfo(new UserInfoImpl());
        LOG.info("Connecting to " + _host + ":" + _port);
        session.connect();
        return session;
    }

    class SocketFactoryWithConnectTimeout implements SocketFactory {

        @Override
        public OutputStream getOutputStream(Socket socket) throws IOException {
            return socket.getOutputStream();
        }

        @Override
        public InputStream getInputStream(Socket socket) throws IOException {
            return socket.getInputStream();
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            Socket socket = new Socket();
            socket.bind(null);
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
            return socket;
        }

    }

    class UserInfoImpl implements UserInfo {

        @Override
        public String getPassphrase() {
            return "";
        }

        @Override
        public String getPassword() {
            return "";
        }

        @Override
        public boolean promptPassphrase(String arg0) {
            return true;
        }

        @Override
        public boolean promptPassword(String arg0) {
            return true;
        }

        @Override
        public boolean promptYesNo(String arg0) {
            return true;
        }

        @Override
        public void showMessage(String message) {
            LOG.info(message);
        }
    }
}
