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

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class DatasetXmlArchisIdentifiersHaveAtMost10Characters implements BagValidatorRule {
    private final XmlReader xmlReader;
    @Override
    public RuleResult validate(Path path) throws Exception {
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

        // points
        var expr = "/ddm:DDM/ddm:dcmiMetadata/dcterms:identifier[@xsi:type = 'id-type:ARCHIS-ZAAK-IDENTIFICATIE']";
        var match = xmlReader.xpathToStreamOfStrings(document, expr)
                .filter(Objects::nonNull)
                .peek(text -> log.trace("Validating element text '{}' for maximum length", text))
                .filter(text -> text.length() > 10)
                .collect(Collectors.toList());

        log.debug("Invalid Archis identifiers: {}", match);

        if (match.size() > 0) {
            var errors = match.stream().map(e -> String.format(
                    "dataset.xml: Archis identifier must be 10 or fewer characters long: %s", e
            )).collect(Collectors.toList());

            return RuleResult.error(errors);
        }

        return RuleResult.ok();
    }
}
