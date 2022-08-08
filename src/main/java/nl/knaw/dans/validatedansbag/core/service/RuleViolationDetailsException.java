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
package nl.knaw.dans.validatedansbag.core.service;

import java.util.ArrayList;
import java.util.List;

public class RuleViolationDetailsException extends Throwable {
    private final List<RuleViolationDetailsException> exceptions = new ArrayList<>();
    private boolean isMultiException = false;

    public RuleViolationDetailsException(String message) {
        super(message);
    }
    public RuleViolationDetailsException(String message, List<RuleViolationDetailsException> exceptions) {
        super(message);
        this.exceptions.addAll(exceptions);
        this.isMultiException = true;
    }

    public RuleViolationDetailsException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleViolationDetailsException(List<RuleViolationDetailsException> exceptions) {
        super();
        this.exceptions.addAll(exceptions);
        this.isMultiException = true;
    }

    public boolean isMultiException() {
        return isMultiException;
    }

    public List<RuleViolationDetailsException> getExceptions() {
        return this.exceptions;
    }
}
