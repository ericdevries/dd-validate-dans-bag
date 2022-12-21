/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.validatedansbag.core.engine;

import java.util.List;

// A simple container for the results of the execution of a single rule
public class RuleResult {
    private final List<String> errorMessages;
    private final Status status;
    private Throwable exception;

    protected RuleResult(Status status, List<String> errorMessages) {
        this.status = status;
        this.errorMessages = errorMessages;
    }

    protected RuleResult(Status status, List<String> errorMessages, Throwable e) {
        this.status = status;
        this.errorMessages = errorMessages;
        this.exception = e;
    }

    public static RuleResult ok() {
        return new RuleResult(Status.SUCCESS, null);
    }

    public static RuleResult error(String message) {
        return new RuleResult(Status.ERROR, List.of(message));
    }

    public static RuleResult error(String message, Throwable e) {
        return new RuleResult(Status.ERROR, List.of(message), e);
    }

    public static RuleResult error(List<String> messages) {
        return new RuleResult(Status.ERROR, messages);
    }

    public static RuleResult skipDependencies() {
        return new RuleResult(Status.SKIP_DEPENDENCIES, null);
    }

    public Throwable getException() {
        return exception;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "RuleResult{" +
            "errorMessages=" + errorMessages +
            ", status=" + status +
            ", exception=" + exception +
            '}';
    }

    public enum Status {
        SUCCESS,
        SKIP_DEPENDENCIES,
        ERROR,
    }
}
