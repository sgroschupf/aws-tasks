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

import java.util.EnumSet;

import com.amazonaws.services.elasticmapreduce.model.JobFlowExecutionStatusDetail;

/**
 * @see JobFlowExecutionStatusDetail#getState()
 */
enum JobFlowState {

    /***/
    COMPLETED(EnumSet.of(StateCategory.DEAD)),
    /***/
    FAILED(EnumSet.of(StateCategory.DEAD)),
    /***/
    TERMINATED(EnumSet.of(StateCategory.DEAD)),
    /***/
    RUNNING(EnumSet.of(StateCategory.OPERATIONAL)),
    /***/
    SHUTTING_DOWN(EnumSet.of(StateCategory.CHANGING_STATE)),
    /***/
    STARTING(EnumSet.of(StateCategory.CHANGING_STATE)),
    /***/
    WAITING(EnumSet.of(StateCategory.OPERATIONAL)),
    /***/
    BOOTSTRAPPING(EnumSet.of(StateCategory.CHANGING_STATE));

    private final EnumSet<StateCategory> _basicStates;

    private JobFlowState(EnumSet<StateCategory> basicStates) {
        _basicStates = basicStates;
    }

    public boolean isIn(StateCategory category) {
        return _basicStates.contains(category);
    }

    public boolean isOperational() {
        return isIn(StateCategory.OPERATIONAL);
    }

    public boolean isDead() {
        return isIn(StateCategory.DEAD);
    }

    public boolean isChangingState() {
        return isIn(StateCategory.CHANGING_STATE);
    }

    public boolean isIdle() {
        return this == WAITING;
    }

    static enum StateCategory {
        CHANGING_STATE, OPERATIONAL, DEAD;
    }

}
