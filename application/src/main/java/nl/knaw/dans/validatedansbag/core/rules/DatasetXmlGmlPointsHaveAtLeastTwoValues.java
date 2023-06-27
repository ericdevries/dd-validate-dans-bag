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
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class DatasetXmlGmlPointsHaveAtLeastTwoValues implements BagValidatorRule {
    private final XmlReader xmlReader;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

        // points
        var expr = "//gml:Point/gml:pos | //gml:lowerCorner | //gml:upperCorner";

        var errors = xmlReader.xpathToStream(document, expr)
                .map(value -> {
                    var attr = value.getParentNode().getAttributes().getNamedItem("srsName");
                    var text = value.getTextContent();
                    var isRD = attr != null && "urn:ogc:def:crs:EPSG::28992".equals(attr.getTextContent());

                    log.trace("Validating point {} (isRD: {})", text, isRD);

                    try {
                        var parts = Arrays.stream(text.split("\\s+"))
                                .map(String::trim)
                                .map(Float::parseFloat)
                                .collect(Collectors.toList());

                        if (parts.size() < 2) {
                            return String.format(
                                    "%s has less than two coordinates: %s", value.getLocalName(), text
                            );
                        } else if (isRD) {
                            var x = parts.get(0);
                            var y = parts.get(1);

                            var valid = x >= -7000 && x <= 300000 && y >= 289000 && y <= 629000;

                            if (!valid) {
                                return String.format(
                                        "%s is outside RD bounds: %s", value.getLocalName(), text
                                );
                            }
                        }
                    } catch (NumberFormatException e) {
                        return String.format(
                                "%s has non numeric coordinates: %s", value.getLocalName(), text
                        );
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Errors while validating points: {}", errors);

        if (errors.size() > 0) {
            return RuleResult.error(errors);
        }

        return RuleResult.ok();
    }
}
