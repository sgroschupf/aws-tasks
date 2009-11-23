package com.dm.awstasks.ssh;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.dm.awstasks.ec2.AbstractEc2IntegrationInteractionTest;
import com.xerox.amazonws.ec2.EC2Exception;

public class SshIntegTest extends AbstractEc2IntegrationInteractionTest {

    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();

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

        File downloadedFile = _folder.newFile("aaaaaaaa.xml");
        File downloadFolder = _folder.getRoot();
        runner.run(new ScpDownloadCommand("~/build2.xml", downloadedFile, false));
        runner.run(new ScpDownloadCommand("~/build", downloadFolder, true));
        assertEquals(uploadedFile.length(), downloadedFile.length());
        assertEquals(countFiles(uploadedFolder), countFiles(new File(downloadFolder, uploadedFolder.getName())));
    }

    private JschRunner createJschRunner() throws EC2Exception, IOException {
        JschRunner jschRunner = new JschRunner("ubuntu", _instanceGroup.getCurrentReservationDescription().getInstances().get(0).getDnsName());
        jschRunner.setKeyfile(_privateKeyFile);
        jschRunner.setTrust(true);
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
