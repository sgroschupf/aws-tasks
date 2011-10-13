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

public class ScpDownload extends AbstractScpCommand {

    private boolean _recursiv;

    public boolean isRecursiv() {
        return _recursiv;
    }

    public void setRecursiv(boolean recursiv) {
        _recursiv = recursiv;
    }

    @Override
    public void execute(Project project, Map<String, String> propertyMap, SshClient sshClient) throws IOException {
        sshClient.downloadFile(getRemotePath(), getLocalFile(), isRecursiv());
    }

    @Override
    public void execute(Project project, Map<String, String> propertyMap, SshClient sshClient, int[] targetInstances) throws IOException {
        sshClient.downloadFile(getRemotePath(), getLocalFile(), isRecursiv(), targetInstances);
    }
}
