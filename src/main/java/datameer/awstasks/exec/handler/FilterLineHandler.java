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

import org.apache.log4j.Logger;

import datameer.awstasks.exec.ExecOutputHandler;

public class FilterLineHandler<T> implements ExecOutputHandler<T> {

    private static final Logger LOG = Logger.getLogger(FilterLineHandler.class);
    private final ExecOutputHandler<T> _backingHandler;
    private final String[] _linePrefixesToFilterOut;

    private FilterLineHandler(ExecOutputHandler<T> backingHandler, String... linePrefixesToFilterOut) {
        _backingHandler = backingHandler;
        _linePrefixesToFilterOut = linePrefixesToFilterOut;
    }

    @Override
    public void handleLine(String line) {
        for (String prefix : _linePrefixesToFilterOut) {
            if (line.startsWith(prefix)) {
                LOG.warn("Filter out line '" + line + "' for filter '" + prefix + "'");
                return;
            }
        }
        _backingHandler.handleLine(line);
    }

    @Override
    public T getResult(int exitValue) {
        return _backingHandler.getResult(exitValue);
    }

    public static <T> ExecOutputHandler<T> decorate(ExecOutputHandler<T> backingHandler, String... linePrefixesToFilterOut) {
        return new FilterLineHandler<T>(backingHandler, linePrefixesToFilterOut);
    }

}
