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

import java.io.IOException;
import java.util.Map;

import org.apache.tools.ant.Project;

import datameer.awstasks.aws.ec2.ssh.SshClient;

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
        return property != null && "true".equals(property.trim());
    }

    static int[] compileTargetInstances(String targetInstancesString, int instanceCount) {
        int[] targetInstances;
        targetInstancesString = targetInstancesString.replaceAll("n", Integer.toString(instanceCount - 1));
        if (targetInstancesString.contains(",")) {
            String[] split = targetInstancesString.split(",");
            targetInstances = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                targetInstances[i] = Integer.parseInt(split[i].trim());
            }
        } else if (targetInstancesString.contains("-")) {
            String[] split = targetInstancesString.split("-");
            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);
            targetInstances = new int[max - min + 1];
            for (int i = 0; i < targetInstances.length; i++) {
                targetInstances[i] = min + i;
            }
        } else {
            targetInstances = new int[1];
            targetInstances[0] = Integer.parseInt(targetInstancesString);
        }

        // check validness
        for (int index : targetInstances) {
            if (index >= instanceCount) {
                throw new IllegalArgumentException("specified '" + index + "' as instance index, but max index is '" + (instanceCount - 1) + "'");
            }
        }
        return targetInstances;
    }

    public void verify(int instanceCount) {
        if (!isToAllInstances()) {
            compileTargetInstances(_targetInstances, instanceCount);
        }
    }

    public void execute(Project project, Map<String, String> propertyMap, SshClient sshClient, int instanceCount) throws IOException {
        if (!isIfFulfilled(project)) {
            System.out.println("skipping command '" + this + "'");
            return;
        }

        if (!isToAllInstances()) {
            int[] targetInstances = compileTargetInstances(_targetInstances, instanceCount);
            execute(project, propertyMap, sshClient, targetInstances);
        } else {
            execute(project, propertyMap, sshClient);
        }

    }

    protected abstract void execute(Project project, Map<String, String> propertyMap, SshClient sshClient) throws IOException;

    protected abstract void execute(Project project, Map<String, String> propertyMap, SshClient sshClient, int[] targetInstances) throws IOException;

}
