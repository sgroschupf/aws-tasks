package datameer.awstasks.ant.ec2;

import org.apache.tools.ant.Project;
import org.junit.Test;

import datameer.awstasks.aws.AbstractAwsIntegrationTest;

public class Ec2ListRunningInstancesTest extends AbstractAwsIntegrationTest {

    @Test
    public void testSshExecutionToAllInstances() throws Exception {
        Project project = new Project();
        Ec2ListRunningInstances task = new Ec2ListRunningInstances();
        task.setProject(project);
        task.setAccessKey(_ec2Conf.getAccessKey());
        task.setAccessSecret(_ec2Conf.getAccessSecret());

        task.execute();
    }
}
