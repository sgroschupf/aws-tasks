package datameer.awstasks.ant.ec2;

import org.apache.tools.ant.BuildException;

import com.xerox.amazonws.ec2.Jec2;

import datameer.awstasks.aws.ec2.InstanceGroup;
import datameer.awstasks.aws.ec2.InstanceGroupImpl;

public abstract class AbstractEc2ConnectTask extends AbstractEc2Task {

    @Override
    public final void execute() throws BuildException {
        Jec2 ec2 = new Jec2(_accessKey, _accessSecret);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
        try {
            instanceGroup.connectTo(_groupName);
            execute(ec2, instanceGroup);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    protected abstract void execute(Jec2 ec2, InstanceGroup instanceGroup) throws Exception;
}
