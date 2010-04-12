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
package datameer.awstasks.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LocalShellExecutor extends ShellExecutor {

    private static String PATH = System.getenv("PATH");
    private static Map<String, String> ENV_MAP = new HashMap<String, String>();
    private File _workingDirectory;

    static {
        // append common pathes
        addPATH("/opt/local/bin");
        addPATH("/usr/bin");
        addEnvironmentVariable("JAVA_HOME", System.getProperty("java.home"), true);
    }

    private static void addPATH(String value) {
        PATH = PATH + ":" + value;
    }

    private static void addEnvironmentVariable(String key, String value, boolean lookupInSystemEnvFirst) {
        if (lookupInSystemEnvFirst) {
            String envValue = System.getenv().get(key);
            if (envValue != null && envValue.trim().length() != 0) {
                value = envValue;
            }
        }
        ENV_MAP.put(key, value);
    }

    public File getWorkingDirectory() {
        return _workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        _workingDirectory = workingDirectory;
    }

    @Override
    public <R> R execute(ShellCommand<?> command, ExecOutputHandler<R> outputHandler) throws IOException {
        int exitValue;
        String[] commandStrings = command.getCommand();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandStrings);
            if (_workingDirectory != null) {
                processBuilder.directory(_workingDirectory);
            }
            processBuilder.redirectErrorStream(true);
            processBuilder.environment().put("PATH", PATH);
            processBuilder.environment().putAll(ENV_MAP);

            Process process = processBuilder.start();
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                outputHandler.handleLine(line);
            }
            input.close();
            process.waitFor();
            exitValue = process.exitValue();
        } catch (Exception e) {
            throw new RuntimeException("exception on executing command '" + Arrays.asList(commandStrings) + "'", e);
        }
        if (exitValue != 0 && command.failOnError()) {
            throw new RuntimeException("could not execute command '" + Arrays.asList(commandStrings) + "', got exit code " + exitValue);
        }
        return outputHandler.getResult(exitValue);

    }

}
