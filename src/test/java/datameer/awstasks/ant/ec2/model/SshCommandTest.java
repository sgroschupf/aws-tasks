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
package datameer.awstasks.ant.ec2.model;

import static org.fest.assertions.Assertions.*;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.junit.Test;

import datameer.awstasks.AbstractTest;
import datameer.awstasks.aws.ec2.ssh.SshClient;

public class SshCommandTest extends AbstractTest {

    SshCommand _sshCommand = new SshCommand() {

        protected void execute(Project project, Map<String, String> propertyMap, SshClient sshClient) throws IOException {
            // nothing todo

        }

        @Override
        protected void execute(Project project, Map<String, String> propertyMap, SshClient sshClient, int[] targetInstances) throws IOException {
            // nothing todo
        }
    };

    @Test
    public void testCompileTargetInstances() {
        assertArrayEquals(new int[] { 0 }, SshCommand.compileTargetInstances("0", 1));
        assertArrayEquals(new int[] { 0, 1, 2 }, SshCommand.compileTargetInstances("0,1,2", 3));
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, SshCommand.compileTargetInstances("0-5", 6));
        assertArrayEquals(new int[] { 1, 2 }, SshCommand.compileTargetInstances("1-2", 3));
        assertArrayEquals(new int[] { 1, 2 }, SshCommand.compileTargetInstances("1-n", 3));
    }

    public void testIsToAll() {
        _sshCommand.setTargetInstances(null);
        assertThat(_sshCommand.isToAllInstances()).isTrue();

        _sshCommand.setTargetInstances("all");
        assertThat(_sshCommand.isToAllInstances()).isTrue();
    }

    @Test(expected = RuntimeException.class)
    public void testCompileWrongConfiguredTargetInstances_1() {
        _sshCommand.setTargetInstances("2");// 0,1 is valid
        _sshCommand.verify(2);
    }

    @Test(expected = RuntimeException.class)
    public void testCompileWrongConfiguredTargetInstances_2() {
        _sshCommand.setTargetInstances("1,2,3");// 0,1,2 is valid
        _sshCommand.verify(3);
    }

}
