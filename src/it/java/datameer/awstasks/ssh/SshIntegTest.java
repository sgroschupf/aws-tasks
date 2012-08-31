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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import datameer.awstasks.aws.ec2.AbstractEc2IntegrationInteractionTest;
import datameer.awstasks.util.IoUtil;

public class SshIntegTest extends AbstractEc2IntegrationInteractionTest {

    private OutputStream _sysOutStream = IoUtil.closeProtectedStream(System.out);

    @Test
    public void testSshExec() throws Exception {
        JschRunner runner = createJschRunner();
        runner.run(new SshExecCommand("ls -l ~/", _sysOutStream));
        try {
            runner.run(new SshExecCommand("rogijeorigjo", _sysOutStream));
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testSshExecWithCommandFile() throws Exception {
        JschRunner runner = createJschRunner();
        File goodCommandFile = _tempFolder.newFile("goodCommandfile");
        File badCommandFile = _tempFolder.newFile("badCommandfile");
        IoUtil.writeFile(goodCommandFile, "ls -l ~/", "echo hostname");
        IoUtil.writeFile(badCommandFile, "erheaefsg", "gergergerg");

        runner.run(new SshExecCommand(goodCommandFile, _sysOutStream));
        try {
            runner.run(new SshExecCommand(badCommandFile, _sysOutStream));
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testScpUpload() throws Exception {
        JschRunner runner = createJschRunner();

        File uploadedFile = new File("build.xml");
        File uploadedFolder = new File("src/build");

        runner.run(new ScpUploadCommand(uploadedFile, "~/"));
        runner.run(new ScpUploadCommand(uploadedFile, "~/build2.xml"));
        runner.run(new ScpUploadCommand(uploadedFolder, "~/"));
    }

    @Test
    public void testScpDownload() throws Exception {
        JschRunner runner = createJschRunner();

        File uploadedFile = new File("build.xml");
        File uploadedFolder = new File("src/build");

        runner.run(new ScpUploadCommand(uploadedFile, "~/"));
        runner.run(new ScpUploadCommand(uploadedFile, "~/build2.xml"));
        runner.run(new ScpUploadCommand(uploadedFolder, "~/"));

        File downloadedFile = _tempFolder.newFile("aaaaaaaa.xml");
        File downloadFolder = _tempFolder.getRoot();
        runner.run(new ScpDownloadCommand("~/build2.xml", downloadedFile, false));
        runner.run(new ScpDownloadCommand("~/build", downloadFolder, true));
        assertEquals(uploadedFile.length(), downloadedFile.length());
        assertEquals(countFiles(uploadedFolder), countFiles(new File(downloadFolder, uploadedFolder.getName())));
    }

    private JschRunner createJschRunner() throws IOException {
        JschRunner jschRunner = new JschRunner(TEST_USERNAME, _instanceGroup.getInstances(false).get(0).getPublicDnsName());
        jschRunner.setKeyfile(new File(_ec2Conf.getPrivateKeyFile()));
        jschRunner.setTrust(true);

        LOG.info("test ssh connections");
        jschRunner.testConnect(TimeUnit.MINUTES.toMillis(5));
        return jschRunner;
    }

    private int countFiles(File folder) {
        AtomicInteger count = new AtomicInteger();
        countFiles(folder, count);
        return count.get();
    }

    private void countFiles(File folder, AtomicInteger count) {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                countFiles(file, count);
            } else {
                count.incrementAndGet();
            }
        }
    }

}
