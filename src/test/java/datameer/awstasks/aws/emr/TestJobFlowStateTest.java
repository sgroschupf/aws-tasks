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

import static org.fest.assertions.Assertions.*;
import junit.framework.TestCase;

import org.junit.Test;

public class TestJobFlowStateTest extends TestCase {

    @Test
    public void testStates_STARTING() throws Exception {
        assertThat(JobFlowState.STARTING.isOperational()).isFalse();
        assertThat(JobFlowState.STARTING.isDead()).isFalse();
        assertThat(JobFlowState.STARTING.isChangingState()).isTrue();
    }

    @Test
    public void testStates_BOOTSTRAPPING() throws Exception {
        assertThat(JobFlowState.BOOTSTRAPPING.isOperational()).isFalse();
        assertThat(JobFlowState.BOOTSTRAPPING.isDead()).isFalse();
        assertThat(JobFlowState.BOOTSTRAPPING.isChangingState()).isTrue();
    }

    @Test
    public void testStates_RUNNING() throws Exception {
        assertThat(JobFlowState.RUNNING.isOperational()).isTrue();
        assertThat(JobFlowState.RUNNING.isDead()).isFalse();
        assertThat(JobFlowState.RUNNING.isChangingState()).isFalse();
    }

    @Test
    public void testStates_WAITING() throws Exception {
        assertThat(JobFlowState.WAITING.isOperational()).isTrue();
        assertThat(JobFlowState.WAITING.isDead()).isFalse();
        assertThat(JobFlowState.WAITING.isChangingState()).isFalse();
    }

    @Test
    public void testStates_SHUTTING_DOWN() throws Exception {
        assertThat(JobFlowState.SHUTTING_DOWN.isOperational()).isFalse();
        assertThat(JobFlowState.SHUTTING_DOWN.isDead()).isFalse();
        assertThat(JobFlowState.SHUTTING_DOWN.isChangingState()).isTrue();
    }

    @Test
    public void testStates_TERMINATED() throws Exception {
        assertThat(JobFlowState.TERMINATED.isOperational()).isFalse();
        assertThat(JobFlowState.TERMINATED.isDead()).isTrue();
        assertThat(JobFlowState.TERMINATED.isChangingState()).isFalse();
    }

    @Test
    public void testStates_FAILED() throws Exception {
        assertThat(JobFlowState.FAILED.isDead()).isTrue();
        assertThat(JobFlowState.FAILED.isChangingState()).isFalse();
        assertThat(JobFlowState.FAILED.isOperational()).isFalse();
    }
}
