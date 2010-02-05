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

import java.util.List;

import datameer.awstasks.exec.ExecOutputHandler;

public class ExecCaptureLineHandler implements ExecOutputHandler<String> {

    private ExecCaptureLinesHandler _captureHandler = new ExecCaptureLinesHandler();

    @Override
    public void handleLine(String line) {
        _captureHandler.handleLine(line);
    }

    public String getResult(int exitCode) {
        List<String> readLines = _captureHandler.getResult(exitCode);
        if (readLines.isEmpty()) {
            return null;
        }
        if (readLines.size() > 2) {
            throw new IllegalStateException("output contain more then 2 lines: " + readLines);
        }
        return readLines.get(0);
    }
}
