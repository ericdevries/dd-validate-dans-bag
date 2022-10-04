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

/**
 * This object is used internally by the RuleEngine to keep track of the status of rules executed
 */
public class RuleValidationResult {

    private final String number;
    private final RuleValidationResultStatus status;
    private final String errorMessage;
    private final boolean shouldSkipDependencies;

    protected RuleValidationResult(String number, RuleValidationResultStatus status, String errorMessage, boolean shouldSkipDependencies) {
        this.number = number;
        this.status = status;
        this.errorMessage = errorMessage;
        this.shouldSkipDependencies = shouldSkipDependencies;

        if (RuleValidationResultStatus.FAILURE.equals(status) && errorMessage == null) {
            throw new RuntimeException("When RuleValidationResultStatus is set to FAIULRE, the error message is required");
        }
    }

    public static RuleValidationResult success(String number) {
        return new RuleValidationResult(number, RuleValidationResultStatus.SUCCESS, null, false);
    }

    public static RuleValidationResult skipDependencies(String number) {
        return new RuleValidationResult(number, RuleValidationResultStatus.SUCCESS, null, true);
    }

    public static RuleValidationResult error(String number, String errorMessage) {
        return new RuleValidationResult(number, RuleValidationResultStatus.FAILURE, errorMessage, false);
    }

    public static RuleValidationResult skipped(String number) {
        return new RuleValidationResult(number, RuleValidationResultStatus.SKIPPED, null, true);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isShouldSkipDependencies() {
        return shouldSkipDependencies;
    }

    public String getNumber() {
        return number;
    }

    public RuleValidationResultStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "RuleValidationResult{" +
            "number='" + number + '\'' +
            ", status=" + status +
            ", errorMessage='" + errorMessage + '\'' +
            ", shouldSkipDependencies=" + shouldSkipDependencies +
            '}';
    }

    public enum RuleValidationResultStatus {
        SUCCESS,
        FAILURE,
        SKIPPED
    }
}
