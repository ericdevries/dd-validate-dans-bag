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
public class DatasetXmlGmlPolygonsInSameMultiSurfaceHaveSameSrsName implements BagValidatorRule {
    private final XmlReader xmlReader;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
        var expr = "//gml:MultiSurface";
        var nodes = xmlReader.xpathToStream(document, expr);
        var match = nodes.filter(node -> {
                    try {
                        var srsNames = xmlReader.xpathToStreamOfStrings(node, ".//gml:Polygon/@srsName")
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                        log.trace("Found unique srsName values: {}", srsNames);
                        if (srsNames.size() > 1) {
                            return true;
                        }
                    } catch (Throwable e) {
                        log.error("Error checking srsNames attribute", e);
                        return true;
                    }

                    return false;
                })
                .collect(Collectors.toList());

        log.debug("Invalid MultiSurface elements that contain polygons with different srsNames: {}", match);

        if (!match.isEmpty()) {
            return RuleResult.error("dataset.xml: Found MultiSurface element containing polygons with different srsNames");
        }

        return RuleResult.ok();
    }
}
