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
package datameer.awstasks.ec2.ant.model;

import static org.junit.Assert.*;

import org.junit.Test;

import datameer.awstasks.ec2.ant.model.SshCommand;

public class SshCommandTest {

    SshCommand _sshCommand = new SshCommand() {
        //
    };

    @Test
    public void testCompileTargetInstances() {
        _sshCommand.setTargetInstances("0");
        assertArrayEquals(new int[] { 0 }, _sshCommand.compileTargetInstances(1));

        _sshCommand.setTargetInstances("0,1,2");
        assertArrayEquals(new int[] { 0, 1, 2 }, _sshCommand.compileTargetInstances(3));

        _sshCommand.setTargetInstances("0-5");
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, _sshCommand.compileTargetInstances(6));

        _sshCommand.setTargetInstances("1-2");
        assertArrayEquals(new int[] { 1, 2 }, _sshCommand.compileTargetInstances(3));

        _sshCommand.setTargetInstances("1-n");
        assertArrayEquals(new int[] { 1, 2 }, _sshCommand.compileTargetInstances(3));
    }

    @Test(expected = RuntimeException.class)
    public void testCompileWrongConfiguredTargetInstances_1() {
        _sshCommand.setTargetInstances("2");
        _sshCommand.compileTargetInstances(2);// 0,1 is valid
    }

    @Test(expected = RuntimeException.class)
    public void testCompileWrongConfiguredTargetInstances_2() {
        _sshCommand.setTargetInstances("1,2,3");
        _sshCommand.compileTargetInstances(3);// 0,1,2 is valid
    }

}
