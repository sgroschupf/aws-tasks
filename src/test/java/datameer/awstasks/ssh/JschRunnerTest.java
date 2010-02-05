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

package datameer.awstasks.ssh;

import static org.hamcrest.Matchers.*;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import datameer.awstasks.exec.command.LsCommand;
import datameer.awstasks.testsupport.junit.CheckBefore;
import datameer.awstasks.testsupport.junit.CheckBeforeRunner;
import datameer.awstasks.util.IoUtil;

/**
 * 
 * In order to run this test successfully passwordless access to localhost needs to ba enabled on
 * your system.
 * 
 */
@RunWith(CheckBeforeRunner.class)
public class JschRunnerTest {

    private static final String USER = System.getProperty("user.name");
    private static final String HOST = "localhost";

    @Rule
    public TemporaryFolder _tempFolder = new TemporaryFolder();

    @CheckBefore
    public void checkBefore() throws Exception {
        JschRunner jschRunner = createJschRunner();
        jschRunner.testConnect();
        // should be succesfull if you can connect passphraseless to localhost
    }

    private JschRunner createJschRunner() {
        JschRunner jschRunner = new JschRunner(USER, HOST);
        jschRunner.setKeyfile(JschRunner.findStandardKeyFile(false).getAbsolutePath());
        return jschRunner;
    }

    @Test
    public void testUpload() throws Exception {
        JschRunner jschRunner = createJschRunner();
        File sourceFile = new File("build.xml");
        File destFile = new File(_tempFolder.newFolder("folder"), "file");
        assertFalse(destFile.exists());

        jschRunner.run(new ScpUploadCommand(sourceFile, destFile.getAbsolutePath()));
        assertTrue(destFile.exists());
        assertEquals(sourceFile.length(), destFile.length());
    }

    @Test
    public void testOpen() throws Exception {
        JschRunner jschRunner = createJschRunner();
        InputStream inputStream = jschRunner.openFile(new File("build.xml").getAbsolutePath());
        int available = inputStream.available();
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();

        assertThat(available, greaterThan(0));
        IoUtil.copyBytes(inputStream, byteOutStream);
        assertEquals(available, byteOutStream.size());
        inputStream.close();
    }

    @Test
    public void testLsCommand() throws Exception {
        JschRunner jschRunner = createJschRunner();
        LsCommand command = new LsCommand("/");
        List<String> result = jschRunner.execute(command);
        assertFalse(result.isEmpty());
    }

}
