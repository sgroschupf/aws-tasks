package com.dm.awstasks;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dm.awstasks.ssh.ScpUploader;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;

public class InstanceInteractionIntegTest extends AbstractIntegrationTest {

    private static InstanceGroupImpl _instanceGroup;
    private static Jec2 _ec2;
    private static boolean CLUSTER_ALREADY_RUNNING = false;// to avoid startup time

    @BeforeClass
    public static void startupInstanceGroup() throws EC2Exception {
        String imageId = "ami-5059be39";
        _ec2 = new Jec2(_accessKeyId, _accessKeySecret);
        if (CLUSTER_ALREADY_RUNNING) {
            LOG.info("using existing instance group");
            List<ReservationDescription> reservationDescriptions = _ec2.describeInstances(Collections.EMPTY_LIST);
            ReservationDescription reservationDescription = null;
            for (ReservationDescription rD : reservationDescriptions) {
                if (rD.getInstances().get(0).getState().equals("running")) {
                    reservationDescription = rD;
                    break;
                }
            }
            if (reservationDescription == null) {
                fail("no reservation description with running instances found - set CLUSTER_ALREADY_RUNNING to false");
            }
            _instanceGroup = new InstanceGroupImpl(_ec2, reservationDescription);
        } else {
            LaunchConfiguration launchConfiguration = new LaunchConfiguration(imageId, 2, 2);
            launchConfiguration.setKeyName(_privateKeyName);
            _instanceGroup = new InstanceGroupImpl(_ec2, launchConfiguration);
            _instanceGroup.startup(TimeUnit.MINUTES, 5);
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

        File localDestinationFolder = createTmpFile("localDestinationFolder");
        localDestinationFolder.mkdirs();
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

        File localDestinationFolder = createTmpFile("localDestinationFolder");
        localDestinationFolder.mkdirs();
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

}
