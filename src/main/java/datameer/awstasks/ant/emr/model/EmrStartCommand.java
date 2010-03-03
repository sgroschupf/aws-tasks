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

import datameer.awstasks.aws.emr.EmrCluster;

public class EmrStartCommand implements EmrCommand {

    private String _privateKeyName;
    private int _instanceCount;
    private String _customParameters;

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

    public void setCustomParameters(String customParameter) {
        _customParameters = customParameter;

    }

    @Override
    public void execute(EmrCluster cluster) throws Exception {
        String[] parameterStrings;
        if (_customParameters.contains("|")) {
            parameterStrings = _customParameters.split("|");
        } else {
            parameterStrings = new String[] { _customParameters };

        }
        for (String parameterString : parameterStrings) {
            String[] key_value = parameterString.split("=");
            cluster.getSettings().getCustomStartParameter().put(key_value[0], key_value[1]);
        }
        cluster.getSettings().setInstanceCount(_instanceCount);
        cluster.startup();
    }

}
