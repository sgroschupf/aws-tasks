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
package datameer.awstasks.ant.ec2;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Ignore;
import org.junit.Test;

public class AntFileExecutionTest {

    @Test
    @Ignore
    // we should use another security group since the other integration tests already started a
    // cluster
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
