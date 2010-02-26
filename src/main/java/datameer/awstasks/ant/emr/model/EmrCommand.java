package datameer.awstasks.ant.emr.model;

import datameer.awstasks.aws.emr.EmrCluster;

public interface EmrCommand {

    void execute(EmrCluster cluster) throws Exception;
}
