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
package datameer.awstasks.aws.concurrent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import datameer.awstasks.util.ExceptionUtil;

public class ObjectLock<T> {

    private Lock _lock = new ReentrantLock(true);
    private Condition _lockReleased = _lock.newCondition();
    private Set<T> _lockedObjects = new HashSet<T>();

    public static <T> ObjectLock<T> create() {
        return new ObjectLock<T>();
    }

    public void unlock(T object) {
        _lock.lock();
        try {
            _lockedObjects.remove(object);
            _lockReleased.signalAll();
        } finally {
            _lock.unlock();
        }
    }

    public void lock(T object) {
        _lock.lock();
        try {
            while (!_lockedObjects.add(object)) {
                try {
                    _lockReleased.await();
                } catch (InterruptedException e) {
                    throw ExceptionUtil.convertToRuntimeException(e);
                }
            }
        } finally {
            _lock.unlock();
        }
    }
}
