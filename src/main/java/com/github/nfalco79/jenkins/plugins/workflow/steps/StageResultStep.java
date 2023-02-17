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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class StageResultStep extends Step {

    private final String message;
    private final String result;

    @DataBoundConstructor
    public StageResultStep(String message, String result) {
        message = Util.fixEmptyAndTrim(message);
        if (message == null) {
            throw new IllegalArgumentException(Messages.StageResult_messageEmpty());
        }
        this.message = message;

        result = Util.fixEmptyAndTrim(result);
        if (result == null || !result.equalsIgnoreCase(Result.fromString(result).toString())) {
            throw new IllegalArgumentException(Messages.StageResult_resultEmpty(result));
        }
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public String getResult() {
        return result;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StageResultStepExecution(this, context);
    }

    private static class StageResultStepExecution extends SynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        private transient final StageResultStep step;

        private StageResultStepExecution(StageResultStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            getContext().get(FlowNode.class).addOrReplaceAction(new WarningAction(Result.fromString(step.result)).withMessage(step.message));
            getContext().get(TaskListener.class).getLogger().println(step.message);
            return null;
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "stageResult";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.StageResult_diplayName();
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, FlowNode.class, TaskListener.class);
            return Collections.unmodifiableSet(context);
        }

        public FormValidation doCheckMessage(@QueryParameter String message) {
            if (Util.fixEmptyAndTrim(message) == null) {
                return FormValidation.error(Messages.StageResult_messageEmpty());
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillResultItems() {
            ListBoxModel r = new ListBoxModel();
            for (Result result : Arrays.asList(Result.SUCCESS, Result.UNSTABLE, Result.FAILURE, Result.NOT_BUILT, Result.ABORTED)) {
                r.add(result.toString());
            }
            return r;
        }
    }
}
