package com.dm.awstasks.ssh;

import java.io.File;

public interface SshExecutor {

    void executeCommand(String command);

    void executeCommandFile(File commandFile);
}
