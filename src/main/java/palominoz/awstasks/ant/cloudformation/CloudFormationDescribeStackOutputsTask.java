package palominoz.awstasks.ant.cloudformation;

import java.util.List;

import org.apache.tools.ant.Task;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.Output;

public class CloudFormationDescribeStackOutputsTask extends CloudFormationDescribeStackTask {
    

    @Override
    protected void doExecute(AmazonCloudFormationAsyncClient cloud) {
        List<Output> outputs = stack(cloud).getOutputs();
        LOG.info("Setting properties for stack " + stack(cloud).getStackName());
        for (Output output : outputs){
            setProperty(output.getOutputKey(), output.getOutputValue());
        }
    }
}
