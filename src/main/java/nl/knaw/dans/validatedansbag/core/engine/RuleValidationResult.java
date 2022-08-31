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

public class RuleValidationResult {

    private final String number;
    private final RuleValidationResultStatus status;
    private final Throwable exception;

    public RuleValidationResult(String number, RuleValidationResultStatus status) {
        this.number = number;
        this.status = status;
        this.exception = null;
    }

    public RuleValidationResult(String number, RuleValidationResultStatus status, Throwable exception) {
        this.number = number;
        this.status = status;
        this.exception = exception;
    }

    public String getNumber() {
        return number;
    }

    public RuleValidationResultStatus getStatus() {
        return status;
    }

    public Throwable getException() {
        return exception;
    }

    public enum RuleValidationResultStatus {
        SUCCESS,
        FAILURE,
        SKIPPED
    }

}
