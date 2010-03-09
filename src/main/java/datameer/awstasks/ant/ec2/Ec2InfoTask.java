package datameer.awstasks.ant.ec2;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;

import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

import datameer.awstasks.aws.ec2.InstanceGroup;

public class Ec2InfoTask extends AbstractEc2ConnectTask {

    private List<Task> _commands = new ArrayList<Task>();

    public void addEcho(Echo task) {
        _commands.add(task);
    }

    @Override
    protected void execute(Jec2 ec2, InstanceGroup instanceGroup) throws Exception {
        System.out.println("executing " + getClass().getSimpleName() + " for group '" + _groupName + "'");
        List<Instance> instances = instanceGroup.getReservationDescription(false).getInstances();
        getProject().setProperty("instances.count", instances.size() + "");
        for (int i = 0; i < instances.size(); i++) {
            getProject().setProperty("instance.host." + i, instances.get(i).getDnsName());
            getProject().setProperty("instance.aim." + i, instances.get(i).getImageId());
            getProject().setProperty("instance.id." + i, instances.get(i).getInstanceId());
            getProject().setProperty("instance.type." + i, instances.get(i).getInstanceType().name());
            getProject().setProperty("instance.state." + i, instances.get(i).getState());
        }

        for (Task task : _commands) {
            task.setProject(getProject());
            task.execute();
        }
    }

}
