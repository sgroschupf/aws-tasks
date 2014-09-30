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

import static org.fest.assertions.Assertions.*;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import datameer.awstasks.aws.ec2.AbstractEc2IntegrationInteractionTest;
import datameer.awstasks.util.IoUtil;
import datameer.com.google.common.collect.Lists;
import datameer.com.google.common.collect.Sets;

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

    /**
     * Use one instance of {@link JschRunner} with session caching enabled concurrently.
     * 
     * @throws Exception
     */
    @Test
    public void testMultithreadedUseOfCachedSession() throws Exception {
        final JschRunner runner = createJschRunner(true);
        assertThat(runner.isSessionCacheEnabled()).isTrue();

        File uploadedFile = new File("build.xml");
        final String fileToDownload = "/tmp/uploadFileForMultithreadedAccess";
        runner.run(new ScpUploadCommand(uploadedFile, fileToDownload));

        final List<Exception> exceptions = Lists.newCopyOnWriteArrayList();
        final List<String> results = Lists.newCopyOnWriteArrayList();
        final int repetitionsPerThread = 10;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < repetitionsPerThread; i++) {
                        // sleep randomly to vary the concurrent timings
                        Thread.sleep(new Random().nextInt(250));

                        // execute a command
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        runner.run(new SshExecCommand("cat /etc/hosts", outputStream));
                        results.add(new String(outputStream.toByteArray()));

                        // download a file
                        File downloadedFile = _tempFolder.newFile(UUID.randomUUID().toString());
                        runner.run(new ScpDownloadCommand(fileToDownload, downloadedFile, false));
                        downloadedFile.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exceptions.add(e);
                }
            }
        };

        // can't increase thread count to high because of
        // http://stackoverflow.com/questions/6947651/is-there-a-limit-to-how-many-channels-can-be-open-per-session-in-jsch
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertThat(exceptions).isEmpty();
        assertThat(results).hasSize(threads.length * repetitionsPerThread);
        assertThat(Sets.newHashSet(results)).hasSize(1);
    }

    private JschRunner createJschRunner() throws IOException {
        return createJschRunner(false);
    }

    private JschRunner createJschRunner(boolean cacheSession) throws IOException {
        JschRunner jschRunner = new JschRunner(TEST_USERNAME, _instanceGroup.getInstances(false).get(0).getPublicDnsName(), cacheSession);
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
