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
package datameer.awstasks.aws.ec2.ssh;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface SshClient {

    void executeCommand(String command, OutputStream outputStream) throws IOException;

    void executeCommand(String command, OutputStream outputStream, int[] targetedInstances) throws IOException;

    void executeCommandFile(File commandFile, OutputStream outputStream) throws IOException;

    void executeCommandFile(File commandFile, OutputStream outputStream, int[] targetedInstances) throws IOException;

    void uploadFile(File localFile, String targetPath) throws IOException;

    void uploadFile(File localFile, String targetPath, int[] targetedInstances) throws IOException;

    void downloadFile(String remoteFile, File localPath, boolean recursiv) throws IOException;

    void downloadFile(String remoteFile, File localPath, boolean recursiv, int[] targetedInstances) throws IOException;

    void setEnableConnectRetries(boolean enable);
}
