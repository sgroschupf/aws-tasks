package com.dm.awstasks.ec2.ssh;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.dm.awstasks.ssh.JschRunner;

public abstract class AbstractEc2SshTool {

    protected static final Logger LOG = Logger.getLogger(AbstractEc2SshTool.class);

    protected final File _privateKey;
    protected final String _username;
    protected final List<String> _hostnames;

    public AbstractEc2SshTool(File privateKey, List<String> hostnames, String username) {
        _privateKey = privateKey;
        _hostnames = hostnames;
        _username = username;
    }

    protected JschRunner createJschRunner(String host) {
        JschRunner runner = new JschRunner(_username, host);
        runner.setKeyfile(_privateKey.getAbsolutePath());
        runner.setTrust(true);
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
