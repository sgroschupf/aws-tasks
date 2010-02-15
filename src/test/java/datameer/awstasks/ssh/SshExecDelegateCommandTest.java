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
package datameer.awstasks.ssh;

import static org.mockito.Mockito.*;

import org.junit.Test;

import datameer.awstasks.exec.ExecOutputHandler;
import datameer.awstasks.ssh.SshExecDelegateCommand.ToLineOutputStream;

public class SshExecDelegateCommandTest {

    @Test
    public void testOutputStream_NoEof() throws Exception {
        ExecOutputHandler handler = mock(ExecOutputHandler.class);
        ToLineOutputStream stream = new ToLineOutputStream(handler);

        stream.write("hello handler".getBytes());
        verify(handler, times(1)).handleLine("hello handler");
        verifyNoMoreInteractions(handler);

        stream.write("hello handler\n".getBytes());
        verify(handler, times(2)).handleLine("hello handler");
        verifyNoMoreInteractions(handler);

        stream.write("\nhello handler".getBytes());
        verify(handler, times(1)).handleLine("");
        verify(handler, times(3)).handleLine("hello handler");
        verifyNoMoreInteractions(handler);

        stream.write("1\n2".getBytes());
        verify(handler, times(1)).handleLine("1");
        verify(handler, times(1)).handleLine("2");
        verifyNoMoreInteractions(handler);

        stream.write("a\nb\n".getBytes());
        verify(handler, times(1)).handleLine("a");
        verify(handler, times(1)).handleLine("b");
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testOutputStream_WithOff() throws Exception {
        byte[] bytes = new byte[4096];
        String text = "drwxr-xr-x   7 jz  staff     238 Jan 26 20:13 src\ndrwxr-xr-x   2 jz  staff      68 Feb  4 22:09 trash";
        byte[] textBytes = text.getBytes();
        int off = 14;
        System.arraycopy(textBytes, 0, bytes, off, textBytes.length);
        ExecOutputHandler handler = mock(ExecOutputHandler.class);
        ToLineOutputStream stream = new ToLineOutputStream(handler);
        stream.write(bytes, off, textBytes.length);

        verify(handler, times(1)).handleLine("drwxr-xr-x   7 jz  staff     238 Jan 26 20:13 src");
        verify(handler, times(1)).handleLine("drwxr-xr-x   2 jz  staff      68 Feb  4 22:09 trash");
        verifyNoMoreInteractions(handler);
    }
}
