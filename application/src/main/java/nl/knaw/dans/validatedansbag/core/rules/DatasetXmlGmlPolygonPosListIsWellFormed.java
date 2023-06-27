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
import nl.knaw.dans.validatedansbag.core.validator.PolygonListValidator;

import java.nio.file.Path;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class DatasetXmlGmlPolygonPosListIsWellFormed implements BagValidatorRule {
    private final XmlReader xmlReader;
    private final PolygonListValidator polygonListValidator;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
        var expr = "//dcx-gml:spatial//gml:posList";//*[local-name() = 'posList']";
        var nodes = xmlReader.xpathToStreamOfStrings(document, expr);

        var match = nodes
                .peek(posList -> log.trace("Validation posList value {}", posList))
                .map(polygonListValidator::validatePolygonList)
                .filter(e -> !e.isValid())
                .map(PolygonListValidator.PolygonValidationResult::getMessage)
                .collect(Collectors.toList());

        log.debug("Invalid posList elements: {}", match);

        if (!match.isEmpty()) {
            var message = String.join("\n", match);
            return RuleResult.error("dataset.xml: Invalid posList: " + message);
        }

        return RuleResult.ok();
    }
}
