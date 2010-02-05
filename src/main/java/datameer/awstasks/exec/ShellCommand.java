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

import datameer.awstasks.exec.handler.ExecVoidHandler;

/**
 * An command for shell execution.
 * 
 * @param <R>
 *            the type of the result
 * 
 */
@SuppressWarnings("unchecked")
public class ShellCommand<R> {

    private final String[] _command;
    private boolean _failOnError;
    private ExecOutputHandler<R> _defaultHandler = (ExecOutputHandler<R>) new ExecVoidHandler();

    public ShellCommand(String[] command, boolean failOnError) {
        _command = command;
        _failOnError = failOnError;
    }

    public String[] getCommand() {
        return _command;
    }

    public void setFailOnError(boolean failOnError) {
        _failOnError = failOnError;
    }

    public boolean failOnError() {
        return _failOnError;
    }

    public void setDefaultHandler(ExecOutputHandler<R> defaultHandler) {
        _defaultHandler = defaultHandler;
    }

    public ExecOutputHandler<R> getDefaultHandler() {
        return _defaultHandler;
    }
}
