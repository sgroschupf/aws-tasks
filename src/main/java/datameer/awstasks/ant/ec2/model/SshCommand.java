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
package datameer.awstasks.ant.ec2.model;

import org.apache.tools.ant.Project;

public abstract class SshCommand {

    private String _targetInstances;
    private String _if;

    public void setTargetInstances(String targetInstances) {
        _targetInstances = targetInstances;
    }

    public String getTargetInstances() {
        return _targetInstances;
    }

    public boolean isToAllInstances() {
        return _targetInstances == null || _targetInstances.trim().equals("all");
    }

    public String getIf() {
        return _if;
    }

    public void setIf(String if1) {
        _if = if1;
    }

    public boolean isIfFulfilled(Project project) {
        String property = project.getProperty(_if);
        if (_if == null || _if.isEmpty()) {
            return true;
        }
        return "true".equals(property.trim());
    }

    public int[] compileTargetInstances(int instanceCount) {
        int[] targetInstances;
        _targetInstances = _targetInstances.replaceAll("n", Integer.toString(instanceCount - 1));
        if (_targetInstances.contains(",")) {
            String[] split = _targetInstances.split(",");
            targetInstances = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                targetInstances[i] = Integer.parseInt(split[i].trim());
            }
        } else if (_targetInstances.contains("-")) {
            String[] split = _targetInstances.split("-");
            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);
            targetInstances = new int[max - min + 1];
            for (int i = 0; i < targetInstances.length; i++) {
                targetInstances[i] = min + i;
            }
        } else {
            targetInstances = new int[1];
            targetInstances[0] = Integer.parseInt(_targetInstances);
        }

        // check validness
        for (int index : targetInstances) {
            if (index >= instanceCount) {
                throw new IllegalArgumentException("specified '" + index + "' as instance index, but max index is '" + (instanceCount - 1) + "'");
            }
        }
        return targetInstances;
    }
}
