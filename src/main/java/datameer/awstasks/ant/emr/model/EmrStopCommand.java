package datameer.awstasks.ant.emr.model;

import datameer.awstasks.aws.emr.EmrCluster;

public class EmrStopCommand implements EmrCommand {

    @Override
    public void execute(EmrCluster cluster) throws Exception {
        if (!cluster.isConnected()) {
            cluster.connect();
        }
        cluster.shutdown();
    }

}
