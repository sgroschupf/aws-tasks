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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.tools.ant.Project;

import datameer.awstasks.aws.ec2.ssh.SshClient;

public class ScpUpload extends AbstractScpCommand {

    private String _localFiles;

    public void setLocalFiles(String localFiles) {
        _localFiles = localFiles;
    }

    public String getLocalFiles() {
        return _localFiles;
    }

    public File[] getLocalFilesAsFile() {
        if (getLocalFiles() == null || getLocalFiles().isEmpty()) {
            return null;
        }
        String[] split = getLocalFiles().split(",");
        File[] files = new File[split.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(split[i]);
        }
        return files;
    }

    @Override
    public void execute(Project project, Map<String, String> propertyMap, SshClient sshClient) throws IOException {
        execute(project, propertyMap, sshClient, null);
    }

    @Override
    public void execute(Project project, Map<String, String> propertyMap, SshClient sshClient, int[] targetInstances) throws IOException {
        if (getLocalFiles() != null && getLocalFile() != null) {
            throw new IllegalStateException("only one of localFile/localFiles can be set");
        }
        File[] localFiles = getLocalFilesAsFile();
        if (localFiles == null) {
            localFiles = new File[] { getLocalFile() };
        }
        for (File localFile : localFiles) {
            if (targetInstances == null) {
                sshClient.uploadFile(localFile, getRemotePath());
            } else {
                sshClient.uploadFile(localFile, getRemotePath(), targetInstances);
            }
        }
    }

}
