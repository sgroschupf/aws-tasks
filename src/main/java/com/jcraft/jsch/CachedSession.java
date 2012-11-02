package com.jcraft.jsch;

public class CachedSession extends Session{

    public CachedSession(JSch jsch) throws JSchException {
        super(jsch);
    }
    
    @Override
    public void setUserName(String username) {
        // Exposing this method
        super.setUserName(username);
    }
    @Override
    public void disconnect() {
        // Don't disconnect the session yet
        //super.disconnect();
    }
    
    @Override
    public String toString() {
        return generateKey(this.host, this.port, this.username);
    }

    public void cleanSession() {
        super.disconnect();
    }
    
    public static String generateKey(String host, int port, String username) {
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(host).append(port).append(username);
        return sBuilder.toString();
    }
}
