package com.jcraft.jsch;

import static org.fest.assertions.Assertions.*;

import org.junit.Test;

public class CachedSessionTest {

    @Test
    public void testUniqueSessionKeyGeneration() throws JSchException {
        Session session1 = new CachedSession("a", "1.1.1.58", 0, "abc", null);
        Session session2 = new CachedSession("aa", "1.1.1.5", 80, "bc", null);
        assertThat(session1.toString()).isEqualTo("1.1.1.58_0_a_abc");
        assertThat(session2.toString()).isEqualTo("1.1.1.5_80_aa_bc");
        assertThat(session1.toString()).isNotEqualTo(session2.toString());
        assertThat(CachedSession.generateKey("a", "1.1.1.58", 0, "abc")).isEqualTo(session1.toString());
    }

}
