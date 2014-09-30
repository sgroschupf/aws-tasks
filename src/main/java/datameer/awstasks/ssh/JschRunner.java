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
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.jcraft.jsch.CachedSession;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityKeyString;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import datameer.awstasks.exec.ExecOutputHandler;
import datameer.awstasks.exec.ShellCommand;
import datameer.awstasks.exec.ShellExecutor;
import datameer.awstasks.util.ExceptionUtil;
import datameer.awstasks.util.Retry;
import datameer.com.google.common.base.Preconditions;
import datameer.com.google.common.base.Throwables;
import datameer.com.google.common.hash.Hashing;
import datameer.com.google.common.io.Files;

public class JschRunner extends ShellExecutor {

    private static final String CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE = "This instance of jsch is already connected please disconnect first.";

    protected static final Logger LOG = Logger.getLogger(JschRunner.class);

    private static final boolean DEFAULT_SESSION_CACHING_ENABLED = false;

    private final String _user;
    private final String _host;
    private int _port = 22;
    private File _keyFile;
    private String _keyFileContent;
    private String _password;
    private String _knownHosts = System.getProperty("user.home") + "/.ssh/known_hosts";
    private boolean _trust;
    protected int _connectTimeout = (int) TimeUnit.SECONDS.toMillis(80);
    private int _timeout = 0;
    private boolean _debug;
    private boolean _enableConnectionRetries;
    private int _createdSessions;
    private String _credentialHash;
    private Properties _config = new Properties();

    private Proxy _proxy = null;

    private CachedSession _cachedSession = null;

    private boolean _sessionCachingEnabled;

    public JschRunner(String user, String host) {
        this(user, host, DEFAULT_SESSION_CACHING_ENABLED);
    }

    public JschRunner(String user, String host, boolean sessionCachingEnabled) {
        _sessionCachingEnabled = sessionCachingEnabled;
        _user = user;
        _host = host;
    }

    public String getHost() {
        return _host;
    }

    public void setKeyfile(File keyfile) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        if (_password != null || _keyFileContent != null) {
            throwAuthenticationAlreadySetException();
        }
        _keyFile = keyfile;
        try {
            _credentialHash = Files.hash(keyfile, Hashing.md5()).toString();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void setKeyfileContent(String keyFileContent) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        if (_password != null || _keyFile != null) {
            throwAuthenticationAlreadySetException();
        }

        _keyFileContent = keyFileContent;
        _credentialHash = Hashing.md5().hashString(keyFileContent, Charset.defaultCharset()).toString();
    }

    public void setPassword(String password) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        if (_keyFile != null || _keyFileContent != null) {
            throwAuthenticationAlreadySetException();
        }
        _password = password;
        _credentialHash = Hashing.md5().hashString(password, Charset.defaultCharset()).toString();
    }

    public void setConfig(Properties config) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        _config = config;
    }

    private static void throwAuthenticationAlreadySetException() {
        throw new IllegalStateException("set either password OR keyfile OR keyfile-content");
    }

    public void setKnownHosts(String knownHosts) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        _knownHosts = knownHosts;
    }

    public void setTrust(boolean trust) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        _trust = trust;
    }

    public void setPort(int port) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        _port = port;
    }

    public int getPort() {
        return _port;
    }

    public void setConnectTimeout(int connectTimeout) {
        _connectTimeout = connectTimeout;
    }

    public int getConnectTimeout() {
        return _connectTimeout;
    }

    public void setTimeout(int timeout) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        _timeout = timeout;
    }

    public int getTimeout() {
        return _timeout;
    }

    public void setDebug(boolean debug) {
        _debug = debug;
    }

    public boolean isDebug() {
        return _debug;
    }

    public int getCreatedSessions() {
        return _createdSessions;
    }

    public void setEnableConnectionRetries(boolean enableConnectionRetries) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        _enableConnectionRetries = enableConnectionRetries;
    }

    public boolean isEnableConnectionRetries() {
        return _enableConnectionRetries;
    }

    public Proxy getProxy() {
        return _proxy;
    }

    public void setProxy(Proxy proxy) {
        Preconditions.checkState(!isConnected(_cachedSession), CHANGE_ON_ALREADY_RUNNING_SESSION_ERROR_MESSAGE);
        _proxy = proxy;
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

    @Override
    public <R> R execute(ShellCommand<?> command, ExecOutputHandler<R> outputHandler) throws IOException {
        SshExecDelegateCommand<R> sshCommand = new SshExecDelegateCommand<R>(command, outputHandler);
        run(sshCommand);
        return sshCommand.getResult();
    }

    public InputStream openFile(String remoteFile) throws IOException {
        Session session = null;
        try {
            session = openSession();
            return new ScpFileInputStream(session, remoteFile);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    public OutputStream createFile(String remoteFile, long length) throws IOException {
        Session session = null;
        try {
            session = openSession();
            return new ScpFileOutputStream(session, remoteFile, length);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    /**
     * Connects to the host and then closes the connection. Throws an exception if connection cannot
     * be established.
     * 
     * @throws IOException
     */
    public void testConnect() throws IOException {
        run(new JschCommand() {
            @Override
            public void execute(Session session) throws IOException {
                // nothing todo
            }
        });
    }

    public void testConnect(long maxWaitTime) throws IOException {
        boolean succeed = false;
        long startTime = System.currentTimeMillis();
        do {
            try {
                testConnect();
                succeed = true;
            } catch (IOException e) {
                LOG.warn("failed to connect with " + _user + "@" + _host + ":" + _port + " :" + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    Thread.interrupted();
                }
            }
        } while (!succeed && (System.currentTimeMillis() - startTime) < maxWaitTime);
        if (!succeed) {
            throw new IOException("failed to establish ssh connection to " + _host);
        }
    }

    private boolean isSessionCacheEnabled() {
        return _sessionCachingEnabled;
    }

    public Session openSession() throws JSchException {
        if (isSessionCacheEnabled()) {
            String cacheKey = CachedSession.generateKey(_user, _host, _port, _credentialHash);
            if (null == _cachedSession || !isConnected(_cachedSession)) {
                LOG.info("Opening new cached session:" + cacheKey);
                _cachedSession = (CachedSession) createFreshSession(true);
            }
            return _cachedSession;
        } else {
            return createFreshSession(false);
        }
    }

    static boolean isConnected(Session cachedSession) {
        if (null == cachedSession || !cachedSession.isConnected()) {
            return false;
        }
        try {
            ChannelExec testChannel = (ChannelExec) cachedSession.openChannel("exec");
            testChannel.connect();
            testChannel.setCommand("true");
            testChannel.disconnect();
            return true;
        } catch (Exception e) {
            LOG.info("Session is connected but cannot be used and needs to be recreated.");
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Session createFreshSession(boolean cached) throws JSchException {
        JSch jsch = new JSch();
        if (isDebug()) {
            JSch.setLogger(DEBUG_LOGGER);
        }
        if (_keyFile != null) {
            jsch.addIdentity(_keyFile.getAbsolutePath());
        }
        if (_keyFileContent != null) {
            Identity identity = IdentityKeyString.newInstance(_keyFileContent, jsch);
            jsch.addIdentity(identity, null);
        }

        if (!_trust && _knownHosts != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using known hosts: " + _knownHosts);
            }
            jsch.setKnownHosts(_knownHosts);
        }

        final Session session;
        if (cached) {
            session = new CachedSession(_user, _host, _port, _credentialHash, jsch);
        } else {
            session = jsch.getSession(_user, _host, _port);
        }
        session.setSocketFactory(new SocketFactoryWithConnectTimeout());
        session.setUserInfo(new UserInfoImpl(_password));
        session.setTimeout(_timeout);
        session.setDaemonThread(true);
        session.setConfig(_config);
        if (_proxy != null) {
            session.setProxy(_proxy);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Connecting to " + _host + ":" + _port);
        }
        if (_enableConnectionRetries) {
            // experimental
            Retry retry = Retry.onExceptions(NoRouteToHostException.class).withMaxRetries(3).withWaitTime(2500);
            retry.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        session.connect();
                    } catch (JSchException e) {
                        throw ExceptionUtil.convertToRuntimeException(e.getCause());
                    }
                }
            });
        } else {
            session.connect();
        }
        LOG.info("Create SSH (cached=" + cached + ") session for Host:" + _host + ":" + _port + " with user:" + _user);
        _createdSessions++;
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
            socket.connect(new InetSocketAddress(host, port), _connectTimeout);
            return socket;
        }

    }

    private static class UserInfoImpl implements UserInfo, UIKeyboardInteractive {

        private final String _password;

        public UserInfoImpl(String password) {
            _password = password;
        }

        @Override
        public String getPassphrase() {
            return "";
        }

        @Override
        public String getPassword() {
            return _password;
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

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
            if (prompt.length != 1 || echo[0] != false || _password == null) {
                return null;
            }
            String[] response = new String[1];
            response[0] = _password;
            return response;

        }
    }

    protected static com.jcraft.jsch.Logger DEBUG_LOGGER = new com.jcraft.jsch.Logger() {

        @Override
        public void log(int level, String message) {
            System.out.println("jsch[" + level + "]: " + message);
        }

        @Override
        public boolean isEnabled(int level) {
            return true;
        }
    };

    public static File findStandardKeyFile(boolean failIfNotFound) {
        String homeFolder = System.getProperty("user.home");
        if (homeFolder == null) {
            if (failIfNotFound) {
                throw new IllegalStateException("no user.home set");
            }
            return null;
        }

        List<File> standardPathes = new ArrayList<File>();
        standardPathes.add(new File(homeFolder, ".ssh/id_rsa"));
        standardPathes.add(new File(homeFolder, ".ssh/id_dsa"));
        for (File file : standardPathes) {
            if (file.exists()) {
                return file;
            }
        }
        if (failIfNotFound) {
            throw new IllegalStateException("no private keyfile found in standard locations: " + standardPathes);
        }
        return null;
    }

    /**
     * Disconnect a cached session. This should always be done if the JschRunner instance is no
     * longer used and was created to cache the created session.
     */
    public void disconnect() {
        if (null != _cachedSession) {
            _cachedSession.forcedDisconnect();
            _cachedSession = null;
        }
    }

}
