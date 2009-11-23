package com.dm.awstasks.ec2.ant;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;

import com.dm.awstasks.ec2.InstanceGroup;
import com.dm.awstasks.ec2.InstanceGroupImpl;
import com.dm.awstasks.ec2.ant.model.ScpDownload;
import com.dm.awstasks.ec2.ant.model.ScpUpload;
import com.dm.awstasks.ec2.ssh.Ec2ScpUploader;
import com.xerox.amazonws.ec2.Jec2;

public class Ec2ScpTask extends AbstractEc2SshTask {

    private List<ScpUpload> _uploads = new ArrayList<ScpUpload>();
    private List<ScpDownload> _downloads = new ArrayList<ScpDownload>();

    public void addUpload(ScpUpload upload) {
        _uploads.add(upload);
    }

    public void addDownload(ScpDownload download) {
        _downloads.add(download);
    }

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName() + " with groupName '" + _groupName + "'");
        Jec2 ec2 = new Jec2(_accessKey, _accessSecret);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
        try {
            instanceGroup.connectTo(_groupName);
            Ec2ScpUploader scpUploader = instanceGroup.createScpUploader(_username, _keyFile);
            for (ScpUpload upload : _uploads) {
                if (upload.isToAllInstances()) {
                    scpUploader.uploadFile(upload.getLocalFile(), upload.getRemotePath());
                } else {
                    scpUploader.uploadFile(upload.getLocalFile(), upload.getRemotePath(), upload.compileTargetInstances());
                }
            }
            for (ScpDownload download : _downloads) {
                if (download.isToAllInstances()) {
                    scpUploader.downloadFile(download.getRemotePath(), download.getLocalFile(), download.isRecursiv());
                } else {
                    scpUploader.downloadFile(download.getRemotePath(), download.getLocalFile(), download.isRecursiv(), download.compileTargetInstances());
                }
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

}
