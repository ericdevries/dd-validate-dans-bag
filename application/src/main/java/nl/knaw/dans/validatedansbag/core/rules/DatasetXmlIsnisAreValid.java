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
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidator;

import java.nio.file.Path;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class DatasetXmlIsnisAreValid implements BagValidatorRule {
    private final XmlReader xmlReader;
    private final IdentifierValidator identifierValidator;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
        var expr = "//dcx-dai:ISNI";
        var match = xmlReader.xpathToStreamOfStrings(document, expr)
                .peek(id -> log.trace("Validating if {} is a valid ISNI", id))
                .filter((id) -> !identifierValidator.validateIsni(id))
                .collect(Collectors.toList());

        log.debug("Identifiers (ISNI) that do not match the pattern: {}", match);

        if (!match.isEmpty()) {
            var message = String.join(", ", match);
            return RuleResult.error("dataset.xml: Invalid ISNI(s): " + message);
        }

        return RuleResult.ok();
    }
}
