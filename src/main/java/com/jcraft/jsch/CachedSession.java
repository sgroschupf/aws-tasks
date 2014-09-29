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
package com.jcraft.jsch;

public class CachedSession extends Session {

    private String _cacheKey;

    public CachedSession(String user, String host, int port, String credentialHash, JSch jsch) throws JSchException {
        super(jsch);
        setUserName(user);
        setHost(host);
        setPort(port);
        _cacheKey = generateKey(user, host, port, credentialHash);
    }

    @Override
    public void disconnect() {
        // do nothing - prevent disconnect
    }

    public void forcedDisconnect() {
        super.disconnect();
    }

    @Override
    public String toString() {
        return _cacheKey;
    }

    public static String generateKey(String username, String host, int port, String credentialHash) {
        return new StringBuilder().append(host).append(port).append(username).append(credentialHash).toString();
    }
}
