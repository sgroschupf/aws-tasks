/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datameer.awstasks.ant.emr.model;

import java.util.ArrayList;
import java.util.List;

import datameer.awstasks.aws.emr.EmrCluster;
import datameer.awstasks.aws.emr.EmrCluster.ClusterState;
import datameer.awstasks.aws.emr.EmrSettings;

public class EmrStartCommand implements EmrCommand {

    private String _privateKeyName;
    private int _instanceCount;
    private String _amiVersion;
    private String _hadoopVersion;
    private boolean _reuseRunningCluster;
    private List<BootstrapConfig> _bootstrapConfigs = new ArrayList<BootstrapConfig>();

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

    public String getHadoopVersion() {
        return _hadoopVersion;
    }

    public String getAmiVersion() {
        return _amiVersion;
    }

    public void setAmiVersion(String amiVersion) {
        _amiVersion = amiVersion;
    }

    public void setHadoopVersion(String hadoopVersion) {
        _hadoopVersion = hadoopVersion;
    }

    public void addBootstrapConfig(BootstrapConfig bootstrapConfig) {
        _bootstrapConfigs.add(bootstrapConfig);
    }

    public void setReuseRunningCluster(boolean reuseRunningCluster) {
        _reuseRunningCluster = reuseRunningCluster;
    }

    public boolean isReuseRunningCluster() {
        return _reuseRunningCluster;
    }

    @Override
    public void execute(EmrCluster cluster) throws Exception {
        EmrSettings settings = cluster.getSettings();
        if (_amiVersion != null) {
            settings.setAmiVersion(_amiVersion);
        }
        if (_hadoopVersion != null) {
            settings.setHadoopVersion(_hadoopVersion);
        }
        settings.setInstanceCount(_instanceCount);
        settings.setPrivateKeyName(_privateKeyName);
        for (BootstrapConfig bootstrapConfig : _bootstrapConfigs) {
            settings.getBootstrapActions().add(bootstrapConfig.createBootstrapActionConfig());
        }
        if (isReuseRunningCluster()) {
            try {
                cluster.connectByName();
            } catch (Exception e) {
                // might not be running
            }
        }
        if (cluster.getState() == ClusterState.UNCONNECTED) {
            cluster.startup();
        }
    }
}
