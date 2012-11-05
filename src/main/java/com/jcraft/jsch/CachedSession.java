package com.jcraft.jsch;

public class CachedSession extends Session {

    private boolean _cacheEnabled = true;

    public CachedSession(JSch jsch) throws JSchException {
        this(jsch, true);
    }

    public CachedSession(JSch jsch, boolean cacheEnabled) throws JSchException {
        super(jsch);
        _cacheEnabled = cacheEnabled;
    }

    @Override
    public void setUserName(String username) {
        // Exposing this method
        super.setUserName(username);
    }

    @Override
    public void disconnect() {
        // Don't disconnect the session yet if the caching is enabled
        if (!_cacheEnabled) {
            super.disconnect();
        }
    }

    @Override
    public String toString() {
        return generateKey(this.host, this.port, this.username);
    }

    public void forcedDisconnect() {
        super.disconnect();
    }

    public static String generateKey(String host, int port, String username) {
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(host).append(port).append(username);
        return sBuilder.toString();
    }
}
