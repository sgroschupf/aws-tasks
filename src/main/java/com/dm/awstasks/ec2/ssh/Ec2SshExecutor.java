package com.dm.awstasks.ec2.ssh;

import java.io.File;

public interface Ec2SshExecutor {

    void executeCommand(String command);

    void executeCommand(String command, int[] targetedInstances);

    void executeCommandFile(File commandFile);

    void executeCommandFile(File commandFile, int[] targetedInstances);
}
