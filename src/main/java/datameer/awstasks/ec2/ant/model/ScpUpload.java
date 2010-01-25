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
package datameer.awstasks.ec2.ant.model;

import java.io.File;

public class ScpUpload extends SshCommand {

    private File _localFile;
    private String _remotePath;

    public File getLocalFile() {
        return _localFile;
    }

    public void setLocalFile(File localFile) {
        _localFile = localFile;
    }

    public String getRemotePath() {
        return _remotePath;
    }

    public void setRemotePath(String remotePath) {
        _remotePath = remotePath;
    }

}
