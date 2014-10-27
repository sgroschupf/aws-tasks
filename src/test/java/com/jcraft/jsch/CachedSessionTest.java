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

import static org.fest.assertions.Assertions.*;

import static org.mockito.Mockito.*;

import org.junit.Test;

public class CachedSessionTest {

    @Test
    public void testUniqueSessionKeyGeneration() throws JSchException {
        JSch jsch = mock(JSch.class);
        Session session1 = new CachedSession("a", "1.1.1.58", 0, "abc", jsch);
        Session session2 = new CachedSession("aa", "1.1.1.5", 80, "bc", jsch);
        assertThat(session1.toString()).isEqualTo("CachedSession{a:abc@1.1.1.58:0}");
        assertThat(session2.toString()).isEqualTo("CachedSession{aa:bc@1.1.1.5:80}");
        assertThat(session1.toString()).isNotEqualTo(session2.toString());
    }

    @Test
    public void testSshUrl() throws Exception {
        assertThat(CachedSession.sshUrl("user", "passwordHash", "host", 888)).isEqualTo("user:passwordHash@host:888");
    }

}
