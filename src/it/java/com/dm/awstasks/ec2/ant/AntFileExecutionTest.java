package com.dm.awstasks.ec2.ant;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Test;

public class AntFileExecutionTest {

    @Test
    public void execute_build_test_xml() throws Exception {
        String command = "ant -f build.test.xml test-ec2";
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // you can inspect log.log for progress
        String line;
        while ((line = stdError.readLine()) != null) {
            System.out.println(line);
        }

        int exitCode = process.waitFor();
        assertEquals(command + " failed", 0, exitCode);
    }
}
