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

import org.apache.log4j.Logger;

import datameer.com.google.common.annotations.VisibleForTesting;
import datameer.com.google.common.base.Predicate;
import datameer.com.google.common.base.Predicates;

public class Retry {

    private static final Logger LOG = Logger.getLogger(Retry.class);
    private long _waitTime = 0;
    private int _maxRetries = 3;
    private final Predicate<Exception> _retryPredicate;
    private int _failedTries;

    private Retry(Predicate<Exception> retryPredicate) {
        _retryPredicate = retryPredicate;
    }

    public Retry withWaitTime(long waitTime) {
        _waitTime = waitTime;
        return this;
    }

    public Retry withMaxRetries(int maxRetries) {
        _maxRetries = maxRetries;
        return this;
    }

    @VisibleForTesting
    int getFailedTries() {
        return _failedTries;
    }

    public void execute(Runnable runnable) {
        _failedTries = 0;
        while (true) {
            try {
                runnable.run();
                return;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                if (_failedTries >= _maxRetries || !_retryPredicate.apply(e)) {
                    throw ExceptionUtil.convertToRuntimeException(e);
                }
                LOG.warn("Failed retry " + (_failedTries + 1) + "/" + _maxRetries + " with '" + e.getMessage() + "' - retrying after " + _maxRetries + " ms");
                
                //TODO jz: remove debug output
                System.out.println("Failed retry " + (_failedTries + 1) + "/" + _maxRetries + " with '" + e.getMessage() + "' - retrying after " + _maxRetries + " ms");
                if (_waitTime > 0) {
                    try {
                        Thread.sleep(_waitTime);
                    } catch (InterruptedException e1) {
                        ExceptionUtil.retainInterruptFlag(e);
                        throw ExceptionUtil.convertToRuntimeException(e);
                    }
                }
                _failedTries++;
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Retry onExceptions(Class<? extends Throwable>... retryTriggeringThrowables) {
        Predicate[] predicates = new Predicate[retryTriggeringThrowables.length];
        for (int i = 0; i < predicates.length; i++) {
            Predicate<?> instanceOf = Predicates.instanceOf(retryTriggeringThrowables[i]);
            predicates[i] = ExceptionUtil.orOnExceptionAndCauses((Predicate<Throwable>) instanceOf);
        }
        return new Retry(Predicates.and(predicates));
    }

}
