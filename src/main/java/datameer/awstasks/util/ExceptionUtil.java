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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ExceptionUtil {

    /**
     * Converts the give exception to a runtime exception. This also resets the interrupt flag, if
     * the given exception is an {@link InterruptedException}.
     * 
     * @param throwable
     * @return
     */
    public static RuntimeException convertToRuntimeException(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            return (RuntimeException) throwable;
        }
        retainInterruptFlag(throwable);
        return new RuntimeException(throwable);
    }

    /**
     * Sets the interrupt flag if the catched exception was an {@link InterruptedException}, because
     * catching an {@link InterruptedException} clears the interrupt flag.
     * 
     * @param throwable
     *            The catched exception.
     */
    public static void retainInterruptFlag(Throwable throwable) {
        if (throwable instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Exception> void throwIfInstance(Throwable throwable, Class<T> exceptionClass) throws T {
        if (exceptionClass.isAssignableFrom(throwable.getClass())) {
            throw (T) throwable;
        }
    }

    public static Map<Thread, StackTraceElement[]> getThreadsWithName(String namePatternString) {
        Pattern namePattern = Pattern.compile(namePatternString);
        Map<Thread, StackTraceElement[]> threadsToStacktraceMap = Thread.getAllStackTraces();

        Set<Thread> keySet = threadsToStacktraceMap.keySet();
        for (Iterator<Thread> iterator = keySet.iterator(); iterator.hasNext();) {
            Thread thread = iterator.next();
            if (!namePattern.matcher(thread.getName()).matches()) {
                iterator.remove();
            }
        }
        return threadsToStacktraceMap;
    }

}
