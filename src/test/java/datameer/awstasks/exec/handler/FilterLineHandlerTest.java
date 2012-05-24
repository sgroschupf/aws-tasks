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

import static org.fest.assertions.Assertions.*;

import java.util.List;

import org.junit.Test;

import datameer.awstasks.exec.ExecOutputHandler;

public class FilterLineHandlerTest {

    @Test
    public void testFilterOnSingleLineHandler() throws Exception {
        ExecOutputHandler<String> lineHandler = new ExecCaptureLineHandler();
        lineHandler = FilterLineHandler.decorate(lineHandler, "filter");

        lineHandler.handleLine("filterLine");
        lineHandler.handleLine("resultLine");
        lineHandler.handleLine("filterLine");
        assertThat(lineHandler.getResult(0)).isEqualTo("resultLine");
    }

    @Test
    public void testFilterOnMultiLineHandler() throws Exception {
        ExecOutputHandler<List<String>> lineHandler = new ExecCaptureLinesHandler();
        lineHandler = FilterLineHandler.decorate(lineHandler, "filter");

        lineHandler.handleLine("filterLine");
        lineHandler.handleLine("resultLine1");
        lineHandler.handleLine("filterLine");
        lineHandler.handleLine("resultLine2");
        lineHandler.handleLine("filterLine");
        assertThat(lineHandler.getResult(0)).hasSize(2).containsExactly("resultLine1", "resultLine2");

    }

}
