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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import datameer.awstasks.ssh.JschRunner;
import datameer.awstasks.ssh.ScpDownloadCommand;
import datameer.awstasks.ssh.ScpUploadCommand;
import datameer.awstasks.ssh.SshExecCommand;

public class SshClientImpl implements SshClient {

    protected static final Logger LOG = Logger.getLogger(SshClientImpl.class);

    protected File _privateKey;
    protected String _password;
    protected final String _username;
    protected final List<String> _hostnames;

    private boolean _enableConnectRetries;

    public SshClientImpl(String username, File privateKey, List<String> hostnames) {
        _username = username;
        _privateKey = privateKey;
        _hostnames = hostnames;
    }

    public SshClientImpl(String username, String password, List<String> hostnames) {
        _username = username;
        _password = password;
        _hostnames = hostnames;
    }

    @Override
    public void setEnableConnectRetries(boolean enable) {
        _enableConnectRetries = enable;
    }

    @Override
    public void executeCommand(String command, OutputStream outputStream) throws IOException {
        executeCommand(_hostnames, command, outputStream);
    }

    @Override
    public void executeCommand(String command, OutputStream outputStream, int[] targetedInstances) throws IOException {
        executeCommand(getHosts(targetedInstances), command, outputStream);
    }

    private void executeCommand(List<String> hostnames, String command, OutputStream outputStream) throws IOException {
        for (String host : hostnames) {
            LOG.info(String.format("executing command '%s' on '%s'", command, host));
            JschRunner jschRunner = createJschRunner(host);
            jschRunner.run(new SshExecCommand(command, outputStream));
        }
    }

    @Override
    public void executeCommandFile(File commandFile, OutputStream outputStream) throws IOException {
        executeCommandFile(_hostnames, commandFile, outputStream);
    }

    @Override
    public void executeCommandFile(File commandFile, OutputStream outputStream, int[] targetedInstances) throws IOException {
        executeCommandFile(getHosts(targetedInstances), commandFile, outputStream);
    }

    private void executeCommandFile(List<String> hostnames, File commandFile, OutputStream outputStream) throws IOException {
        for (String host : hostnames) {
            LOG.info(String.format("executing command-file '%s' on '%s'", commandFile.getAbsolutePath(), host));
            JschRunner jschRunner = createJschRunner(host);
            jschRunner.run(new SshExecCommand(commandFile, outputStream));
        }
    }

    public void uploadFile(File localFile, String targetPath) throws IOException {
        uploadFile(_hostnames, localFile, targetPath);
    }

    @Override
    public void uploadFile(File localFile, String targetPath, int[] instanceIndex) throws IOException {
        List<String> hostnames = getHosts(instanceIndex);
        uploadFile(hostnames, localFile, targetPath);
    }

    private void uploadFile(List<String> hosts, File localFile, String targetPath) throws IOException {
        for (String host : hosts) {
            LOG.info(String.format("uploading file '%s' to '%s'", localFile.getAbsolutePath(), constructRemotePath(host, targetPath)));
            JschRunner jschRunner = createJschRunner(host);
            jschRunner.run(new ScpUploadCommand(localFile, targetPath));
        }
    }

    public void downloadFile(String remoteFile, File localPath, boolean recursiv) throws IOException {
        downloadFiles(_hostnames, remoteFile, localPath, recursiv);
    }

    @Override
    public void downloadFile(String remoteFile, File localPath, boolean recursiv, int[] instanceIndex) throws IOException {
        List<String> hosts = getHosts(instanceIndex);
        downloadFiles(hosts, remoteFile, localPath, recursiv);
    }

    private void downloadFiles(List<String> hostnames, String remoteFile, File localPath, boolean recursiv) throws IOException {
        for (String host : hostnames) {
            LOG.info(String.format("downloading file '%s' to '%s'", constructRemotePath(host, remoteFile), localPath.getAbsolutePath()));
            JschRunner jschRunner = createJschRunner(host);
            jschRunner.run(new ScpDownloadCommand(remoteFile, localPath, recursiv));
        }
    }

    private String constructRemotePath(String host, String filePath) {
        return _username + ":" + "@" + host + ":" + filePath;
    }

    protected JschRunner createJschRunner(String host) {
        JschRunner runner = new JschRunner(_username, host);
        if (_privateKey != null) {
            runner.setKeyfile(_privateKey);
        } else {
            runner.setPassword(_password);
        }
        runner.setTrust(true);
        runner.setEnableConnectionRetries(_enableConnectRetries);
        return runner;
    }

    protected List<String> getHosts(int[] instanceIndex) {
        List<String> hostnames = new ArrayList<String>(_hostnames.size());
        for (int i = 0; i < instanceIndex.length; i++) {
            hostnames.add(_hostnames.get(instanceIndex[i]));
        }
        return hostnames;
    }

}
