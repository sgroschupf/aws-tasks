package com.dm.awstasks.ec2.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;

import com.dm.awstasks.ec2.InstanceGroup;
import com.dm.awstasks.ec2.InstanceGroupImpl;
import com.dm.awstasks.ec2.ssh.Ec2ScpUploader;
import com.xerox.amazonws.ec2.Jec2;

public class Ec2ScpTask extends AbstractEc2SshTask {

    private List<Upload> _uploads = new ArrayList<Upload>();

    public void addUpload(Upload upload) {
        _uploads.add(upload);
    }

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName() + " with groupName '" + _groupName + "'");
        Jec2 ec2 = new Jec2(_accessKey, _accessSecret);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
        try {
            instanceGroup.connectTo(_groupName);
            Ec2ScpUploader scpUploader = instanceGroup.createScpUploader(_keyFile, _username);
            for (Upload upload : _uploads) {
                if (upload.isToAllInstances()) {
                    scpUploader.uploadFile(upload.getLocalFile(), upload.getRemotePath());
                } else {
                    scpUploader.uploadFile(upload.getLocalFile(), upload.getRemotePath(), upload.compileTargetInstances());
                }
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public static class Upload {

        private File _localFile;
        private String _remotePath;
        private String _targetInstances;

        public boolean isToAllInstances() {
            return _targetInstances == null || _targetInstances.trim().equals("all");
        }

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

        public String getTargetInstances() {
            return _targetInstances;
        }

        public int[] compileTargetInstances() {
            int[] targetInstances;
            if (_targetInstances.contains(",")) {
                String[] split = _targetInstances.split(",");
                targetInstances = new int[_targetInstances.length()];
                for (int i = 0; i < split.length; i++) {
                    targetInstances[0] = Integer.parseInt(split[i]);
                }
            } else if (_targetInstances.contains("-")) {
                String[] split = _targetInstances.split("-");
                int min = Integer.parseInt(split[0]);
                int max = Integer.parseInt(split[1]);
                targetInstances = new int[max - min];
                for (int i = 0; i < targetInstances.length; i++) {
                    targetInstances[i] = min + i;
                }
            } else {
                targetInstances = new int[1];
                targetInstances[0] = Integer.parseInt(_targetInstances);
            }
            return targetInstances;
        }

        public void setTargetInstances(String targetInstances) {
            _targetInstances = targetInstances;
        }

    }
}
