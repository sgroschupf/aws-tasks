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
package datameer.awstasks.aws.emr;

public enum JobFlowState {

    COMPLETED(false), FAILED(false), TERMINATED(false), RUNNING(true), SHUTTING_DOWN(false), STARTING(false), WAITING(true);

    private final boolean _operational;

    private JobFlowState(boolean operational) {
        _operational = operational;
    }

    public boolean isOperational() {
        return _operational;
    }

}
