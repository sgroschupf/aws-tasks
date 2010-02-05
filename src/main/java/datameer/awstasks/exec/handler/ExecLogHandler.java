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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import datameer.awstasks.exec.ExecOutputHandler;

public class ExecLogHandler implements ExecOutputHandler<Void> {

    private static Logger LOG = Logger.getLogger(ExecLogHandler.class);
    private final Level _logLevel;

    public ExecLogHandler(Level logLevel) {
        _logLevel = logLevel;
    }

    public void handleLine(String line) {
        LOG.log(_logLevel, line);
    }

    @Override
    public Void getResult(int exitCode) {
        return null;
    }

}
