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
package nl.knaw.dans.validatedansbag.core.validator;

import nl.knaw.dans.validatedansbag.core.config.OtherIdPrefix;

import java.util.List;

public class OrganizationIdentifierPrefixValidatorImpl implements OrganizationIdentifierPrefixValidator {
    private final List<OtherIdPrefix> otherIdPrefixes;

    public OrganizationIdentifierPrefixValidatorImpl(List<OtherIdPrefix> otherIdPrefixes) {
        this.otherIdPrefixes = otherIdPrefixes;
    }

    @Override
    public boolean hasValidPrefix(String user, String identifier) {
        for (var prefix : otherIdPrefixes) {
            if (user.equals(prefix.getUser()) && identifier.startsWith(prefix.getPrefix())) {
                return true;
            }
        }

        return false;
    }
}
