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
package datameer.awstasks.util;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

public class ExceptionUtilTest {

    @Test
    public void testConvertToRuntimeException() {
        RuntimeException runtimeException = new RuntimeException();
        Exception checkedException = new Exception();
        assertSame(runtimeException, ExceptionUtil.convertToRuntimeException(runtimeException));
        assertSame(checkedException, ExceptionUtil.convertToRuntimeException(checkedException).getCause());
    }

    @Test
    public void testRetainInterrupt() {
        ExceptionUtil.retainInterruptFlag(new InterruptedException());
        assertTrue(Thread.currentThread().isInterrupted());
        // clear interrupt flag
        Thread.interrupted();

        ExceptionUtil.retainInterruptFlag(new NullPointerException());
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testThrowIfInstance() throws IOException {
        ExceptionUtil.throwIfInstance(new IOException(), IllegalStateException.class);
        ExceptionUtil.throwIfInstance(new IllegalStateException(), IOException.class);

        try {
            ExceptionUtil.throwIfInstance(new IOException(), IOException.class);
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
        try {
            ExceptionUtil.throwIfInstance(new FileNotFoundException(), IOException.class);
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testThrowIfInstanceII() throws IOException {
        try {
            new BufferedReader(new InputStreamReader(new FileInputStream(new File("dwefsf"))));
            fail("should throw exception");
        } catch (Exception e) {
            ExceptionUtil.throwIfInstance(e, IOException.class);
        }
    }

}
