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

import static org.fest.assertions.Assertions.*;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

@SuppressWarnings("unchecked")
public class RetryTest {

    @Test
    public void testRetry_Successful() throws Exception {
        int maxRetries = 2;
        for (int i = 0; i < maxRetries + 1; i++) {
            Retry retry = Retry.onExceptions(IOException.class).withMaxRetries(maxRetries).withWaitTime(100);
            retry.execute(newFailingRunnable(new IOException(), i));
            assertThat(retry.getFailedTries()).isEqualTo(i);
        }
    }

    @Test
    public void testRetry_Failure() throws Exception {
        Retry retry = Retry.onExceptions(IOException.class).withMaxRetries(2).withWaitTime(100);
        IOException exception = new IOException();
        try {
            retry.execute(newFailingRunnable(exception, 3));
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getCause()).isEqualTo(exception);
        }
        assertThat(retry.getFailedTries()).isEqualTo(2);
    }

    @Test
    public void testNotRetrying() throws Exception {
        Retry retry = Retry.onExceptions(IOException.class).withMaxRetries(3).withWaitTime(100);
        RuntimeException exception = new RuntimeException();
        try {
            retry.execute(newFailingRunnable(exception, 1));
            fail("should throw exception");
        } catch (Throwable e) {
            assertThat(e).isEqualTo(exception);
        }
    }

    private Runnable newFailingRunnable(final Exception exception, final int failTimes) {
        return new Runnable() {
            int failedTimes = 0;

            @Override
            public void run() {
                if (failedTimes < failTimes) {
                    failedTimes++;
                    throw ExceptionUtil.convertToRuntimeException(exception);
                }
            }
        };
    }
}
