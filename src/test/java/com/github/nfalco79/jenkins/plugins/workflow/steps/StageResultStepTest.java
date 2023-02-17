/*
 * Copyright 2022 Nikolas Falco
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.jenkins.plugins.workflow.steps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Result;

public class StageResultStepTest {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void smokes() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class);
        p.setDefinition(new CpsFlowDefinition("stageResult(message: 'Can not approve the pull request', result: 'UNSTABLE')", true));
        WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertWarning(b, "Can not approve the pull request");
    }

    @Test
    public void messageRequired() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class);
        p.setDefinition(new CpsFlowDefinition("stageResult()", true));
        WorkflowRun b = r.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        r.assertLogContains(Messages.StageResult_messageEmpty(), b);
    }

    @Test
    public void resultRequired() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class);
        p.setDefinition(new CpsFlowDefinition("stageResult(message: 'Hello world')", true));
        WorkflowRun b = r.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        r.assertLogContains(Messages.StageResult_resultEmpty(null), b);
    }

    private void assertWarning(WorkflowRun run, String expectedMessage) throws Exception {
        r.assertLogContains(expectedMessage, run);
        FlowExecution exec = run.getExecution();
        FlowNode warningNode = new DepthFirstScanner().findFirstMatch(exec, node -> node.getPersistentAction(WarningAction.class) != null);
        assertThat(warningNode, notNullValue());
        WarningAction warning = warningNode.getPersistentAction(WarningAction.class);
        assertThat(warning, notNullValue());
        assertThat(warning.getMessage(), equalTo(expectedMessage));
    }
}
