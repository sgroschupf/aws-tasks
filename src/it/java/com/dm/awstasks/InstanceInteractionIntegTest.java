package com.dm.awstasks;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
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
    private static String RUNNING_INSTANCE_ID = "";// fill to avoid startup time

    @BeforeClass
    public static void startupInstanceGroup() throws EC2Exception {
        String imageId = "ami-5059be39";
        _ec2 = new Jec2(_accessKeyId, _accessKeySecret);

        if (isRunningInstanceIdSet()) {
            LOG.info("using existing instance group");
            ReservationDescription reservationDescription = _ec2.describeInstances(Arrays.asList(RUNNING_INSTANCE_ID)).get(0);
            _instanceGroup = new InstanceGroupImpl(_ec2, reservationDescription);
        } else {
            LaunchConfiguration launchConfiguration = new LaunchConfiguration(imageId, 2, 2);
            launchConfiguration.setKeyName(_privateKeyName);
            _instanceGroup = new InstanceGroupImpl(_ec2, launchConfiguration);
            _instanceGroup.startup(TimeUnit.MINUTES, 5);
        }

    }

    private static boolean isRunningInstanceIdSet() {
        return !RUNNING_INSTANCE_ID.equals("");
    }

    @AfterClass
    public static void shutdownInstanceGroup() throws EC2Exception {
        if (isRunningInstanceIdSet()) {
            LOG.info("don't shutdown instance group");
        } else {
            _instanceGroup.shutdown();
        }
    }

    @Test
    public void testScpUpload() throws Exception {
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

}
