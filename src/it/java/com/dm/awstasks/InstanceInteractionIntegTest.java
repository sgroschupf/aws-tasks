package com.dm.awstasks;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.dm.awstasks.ssh.ScpUploader;
import com.dm.awstasks.ssh.SshExecutor;
import com.dm.awstasks.util.Ec2Util;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;

public class InstanceInteractionIntegTest extends AbstractIntegrationTest {

    private static InstanceGroupImpl _instanceGroup;
    private static Jec2 _ec2;
    private static boolean CLUSTER_ALREADY_RUNNING = false;// to avoid startup time
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();

    @BeforeClass
    public static void startupInstanceGroup() throws EC2Exception {
        _ec2 = new Jec2(_accessKeyId, _accessKeySecret);
        _instanceGroup = new InstanceGroupImpl(_ec2);

        if (CLUSTER_ALREADY_RUNNING) {
            LOG.info("try to use existing instance group");
            ReservationDescription reservationDescription = Ec2Util.findByGroup(_ec2, "default", "running");
            if (reservationDescription == null) {
                LOG.warn("reservation description with running instances NOT found - starting instance group");
            } else {
                LOG.warn("reservation description with running instances FOUND - using instance group");
                _instanceGroup.connectTo(reservationDescription);
            }
        }
        if (!_instanceGroup.isAssociated()) {
            _instanceGroup.startup(createLaunchConfiguration(2), TimeUnit.MINUTES, 5);
        }
    }

    @AfterClass
    public static void shutdownInstanceGroup() throws EC2Exception {
        if (CLUSTER_ALREADY_RUNNING) {
            LOG.info("don't shutdown instance group");
        } else {
            _instanceGroup.shutdown();
        }
    }

    @Test
    public void testScpUploadToAllInstances() throws Exception {
        File privateKeyFile = new File(_privateKeyFile);
        ScpUploader scpUploader = _instanceGroup.createScpUploader(privateKeyFile, "ubuntu");
        File localFile = new File("README.markdown");
        String remoteDir = "~/";
        scpUploader.uploadFile(localFile, remoteDir);

        File localDestinationFolder = _folder.newFolder("localDestinationFolder");
        scpUploader.downloadFile(remoteDir + localFile.getName(), localDestinationFolder);
        assertEquals(1, localDestinationFolder.list().length);
        assertEquals(localFile.length(), new File(localDestinationFolder, localFile.getName()).length());
    }

    @Test
    public void testScpUploadToSpecificInstances() throws Exception {
        File privateKeyFile = new File(_privateKeyFile);
        ScpUploader scpUploader1 = _instanceGroup.createScpUploader(privateKeyFile, "ubuntu", new int[] { 0 });
        File localFile = new File("build.xml");
        String remoteDir = "~/";
        scpUploader1.uploadFile(localFile, remoteDir);

        File localDestinationFolder = _folder.newFolder("localDestinationFolder");
        scpUploader1.downloadFile(remoteDir + localFile.getName(), localDestinationFolder);
        assertEquals(1, localDestinationFolder.list().length);
        assertEquals(localFile.length(), new File(localDestinationFolder, localFile.getName()).length());

        ScpUploader scpUploader2 = _instanceGroup.createScpUploader(privateKeyFile, "ubuntu", new int[] { 1 });
        try {
            scpUploader2.downloadFile(remoteDir + localFile.getName(), localDestinationFolder);
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testSshExecutionToAllInstances() throws Exception {
        File privateKeyFile = new File(_privateKeyFile);
        SshExecutor sshExecutor = _instanceGroup.createSshExecutor(privateKeyFile, "ubuntu");
        sshExecutor.executeCommand("ls -l");
        String noneExistingFile = "abcfi";
        try {
            sshExecutor.executeCommand("rm " + noneExistingFile);
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
        sshExecutor.executeCommand("touch " + noneExistingFile);
        sshExecutor.executeCommand("rm " + noneExistingFile);
    }

    @Test
    public void testShhExecutionToSpecificInstances() throws Exception {
        File privateKeyFile = new File(_privateKeyFile);
        SshExecutor sshExecutor1 = _instanceGroup.createSshExecutor(privateKeyFile, "ubuntu", new int[] { 0 });
        SshExecutor sshExecutor2 = _instanceGroup.createSshExecutor(privateKeyFile, "ubuntu", new int[] { 1 });

        String noneExistingFile = "abcfi";
        sshExecutor1.executeCommand("touch " + noneExistingFile);
        sshExecutor1.executeCommand("rm " + noneExistingFile);
        try {
            sshExecutor2.executeCommand("rm " + noneExistingFile);
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testSshExecutionFromFile() throws Exception {
        File privateKeyFile = new File(_privateKeyFile);
        SshExecutor sshExecutor = _instanceGroup.createSshExecutor(privateKeyFile, "ubuntu", new int[] { 0 });
        File commandFile = _folder.newFile("commands.txt");
        FileWriter fileWriter = new FileWriter(commandFile);
        fileWriter.write("ls -l\n");
        String noneExistingFile = "abcfi";
        fileWriter.write("touch " + noneExistingFile + "\n");
        fileWriter.write("rm " + noneExistingFile + "\n");
        fileWriter.close();
        sshExecutor.executeCommandFile(commandFile);
    }

}
