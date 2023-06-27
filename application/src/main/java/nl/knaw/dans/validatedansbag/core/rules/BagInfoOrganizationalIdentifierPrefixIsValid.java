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
package nl.knaw.dans.validatedansbag.core.rules;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.validator.OrganizationIdentifierPrefixValidator;

import java.nio.file.Path;

@AllArgsConstructor
@Slf4j
public class BagInfoOrganizationalIdentifierPrefixIsValid implements BagValidatorRule {
    private final BagItMetadataReader bagItMetadataReader;
    private final OrganizationIdentifierPrefixValidator organizationIdentifierPrefixValidator;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var hasOrganizationalIdentifier = bagItMetadataReader.getSingleField(path, "Has-Organizational-Identifier");

        log.debug("Checking prefix on organizational identifier '{}'", hasOrganizationalIdentifier);

        var isValid = organizationIdentifierPrefixValidator.hasValidPrefix(hasOrganizationalIdentifier);

        if (!isValid) {
            return RuleResult.error(String.format("No valid prefix given for value of 'Has-Organizational-Identifier': %s", hasOrganizationalIdentifier));
        }

        return RuleResult.ok();
    }
}
