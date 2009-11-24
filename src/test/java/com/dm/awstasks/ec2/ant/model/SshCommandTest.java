package com.dm.awstasks.ec2.ant.model;

import static org.junit.Assert.*;

import org.junit.Test;

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
