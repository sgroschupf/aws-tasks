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
package datameer.awstasks.exec.handler;

import datameer.awstasks.exec.ExecOutputHandler;

public class ExecChainHandler<R> implements ExecOutputHandler<R> {

    private final ExecOutputHandler<R> _rootHandler;
    private final ExecOutputHandler<?>[] _chainedHandlers;

    public ExecChainHandler(ExecOutputHandler<R> rootHandler, ExecOutputHandler<?>... chainedHandlers) {
        _rootHandler = rootHandler;
        _chainedHandlers = chainedHandlers;
    }

    @Override
    public R getResult(int exitValue) {
        return _rootHandler.getResult(exitValue);
    }

    @Override
    public void handleLine(String line) {
        _rootHandler.handleLine(line);
        for (ExecOutputHandler<?> execOutputHandler : _chainedHandlers) {
            execOutputHandler.handleLine(line);
        }
    }

}
