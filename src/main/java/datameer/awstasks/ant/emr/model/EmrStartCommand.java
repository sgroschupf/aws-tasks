package datameer.awstasks.ant.emr.model;

import datameer.awstasks.aws.emr.EmrCluster;

public class EmrStartCommand implements EmrCommand {

    private String _privateKeyName;
    private int _instanceCount;

    public String getPrivateKeyName() {
        return _privateKeyName;
    }

    public void setPrivateKeyName(String privateKeyName) {
        _privateKeyName = privateKeyName;
    }

    public int getInstanceCount() {
        return _instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        _instanceCount = instanceCount;
    }

    @Override
    public void execute(EmrCluster cluster) throws Exception {
        cluster.startup(_instanceCount, _privateKeyName);
    }

}
