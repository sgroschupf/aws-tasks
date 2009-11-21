package com.dm.awstasks.ec2;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.dm.awstasks.ec2.ssh.Ec2ScpUploader;
import com.dm.awstasks.ec2.ssh.Ec2SshExecutor;

public class InstanceInteractionIntegTest extends AbstractEc2IntegrationInteractionTest {

    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();

    @Test
    public void testScpUploadToAllInstances() throws Exception {
        File privateKeyFile = new File(_privateKeyFile);
        Ec2ScpUploader scpUploader = _instanceGroup.createScpUploader(privateKeyFile, "ubuntu");
        File localFile = new File("README.markdown");
        String remoteDir = "~/";
        scpUploader.uploadFile(localFile, remoteDir);

        File localDestinationFolder = _folder.newFolder("localDestinationFolder");
        scpUploader.downloadFile(remoteDir + localFile.getName(), localDestinationFolder, false);
        assertEquals(1, localDestinationFolder.list().length);
        assertEquals(localFile.length(), new File(localDestinationFolder, localFile.getName()).length());
    }

    @Test
    public void testScpUploadToSpecificInstances() throws Exception {
        File privateKeyFile = new File(_privateKeyFile);
        Ec2ScpUploader scpUploader = _instanceGroup.createScpUploader(privateKeyFile, "ubuntu");
        File localFile = new File("build.xml");
        String remoteDir = "~/";
        scpUploader.uploadFile(localFile, remoteDir, new int[] { 0 });

        File localDestinationFolder = _folder.newFolder("localDestinationFolder");
        scpUploader.downloadFile(remoteDir + localFile.getName(), localDestinationFolder, false, new int[] { 0 });
        assertEquals(1, localDestinationFolder.list().length);
        assertEquals(localFile.length(), new File(localDestinationFolder, localFile.getName()).length());

        try {
            scpUploader.downloadFile(remoteDir + localFile.getName(), localDestinationFolder, false, new int[] { 1 });
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testSshExecutionToAllInstances() throws Exception {
        File privateKeyFile = new File(_privateKeyFile);
        Ec2SshExecutor sshExecutor = _instanceGroup.createSshExecutor(privateKeyFile, "ubuntu");
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
        Ec2SshExecutor sshExecutor1 = _instanceGroup.createSshExecutor(privateKeyFile, "ubuntu");

        String noneExistingFile = "abcfi";
        sshExecutor1.executeCommand("touch " + noneExistingFile, new int[] { 0 });
        sshExecutor1.executeCommand("rm " + noneExistingFile, new int[] { 0 });
        try {
            sshExecutor1.executeCommand("rm " + noneExistingFile, new int[] { 1 });
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testSshExecutionFromFile() throws Exception {
        File privateKeyFile = new File(_privateKeyFile);
        Ec2SshExecutor sshExecutor = _instanceGroup.createSshExecutor(privateKeyFile, "ubuntu");
        File commandFile = _folder.newFile("commands.txt");
        FileWriter fileWriter = new FileWriter(commandFile);
        fileWriter.write("ls -l\n");
        String noneExistingFile = "abcfi";
        fileWriter.write("touch " + noneExistingFile + "\n");
        fileWriter.write("rm " + noneExistingFile + "\n");
        fileWriter.close();
        sshExecutor.executeCommandFile(commandFile, new int[] { 0 });
    }

}
