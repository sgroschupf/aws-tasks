package com.jcraft.jsch;

import static org.fest.assertions.Assertions.*;

import org.junit.Test;

public class CachedSessionTest {

    @Test
    public void testUniqueSessionKeyGeneration() throws JSchException {
        Session session1 = new CachedSession("a", "1.1.1.58", 0, "abc", null);
        Session session2 = new CachedSession("aa", "1.1.1.5", 80, "bc", null);
        assertThat(session1.toString()).isEqualTo("CachedSession{a:abc@1.1.1.58:0}");
        assertThat(session2.toString()).isEqualTo("CachedSession{aa:bc@1.1.1.5:80}");
        assertThat(session1.toString()).isNotEqualTo(session2.toString());
    }

    @Test
    public void testSshUrl() throws Exception {
        assertThat(CachedSession.sshUrl("user", "passwordHash", "host", 888)).isEqualTo("user:passwordHash@host:888");
    }

}
