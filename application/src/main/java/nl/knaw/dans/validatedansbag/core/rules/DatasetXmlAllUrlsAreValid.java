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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Slf4j
public class DatasetXmlAllUrlsAreValid implements BagValidatorRule {
    private final XmlReader xmlReader;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

        var hrefNodes = xmlReader.xpathToStreamOfStrings(document, "//*/@href");
        var schemeURINodes = xmlReader.xpathToStreamOfStrings(document, "//ddm:subject/@schemeURI");
        var valueURINodes = xmlReader.xpathToStreamOfStrings(document, "//ddm:subject/@valueURI");

        var expr = List.of(
                "//*[@xsi:type='dcterms:URI']",
                "//*[@xsi:type='dcterms:URL']",
                "//*[@xsi:type='URI']",
                "//*[@xsi:type='URL']",
                "//*[@scheme='dcterms:URI']",
                "//*[@scheme='dcterms:URL']",
                "//*[@scheme='URI']",
                "//*[@scheme='URL']"
        );

        var elementSelectors = xmlReader.xpathsToStreamOfStrings(document, expr);

        var errors = Stream.of(hrefNodes, schemeURINodes, valueURINodes, elementSelectors)
                .flatMap(i -> i)
                .map(value -> {
                    log.trace("Validating URI '{}'", value);

                    try {
                        var uri = new URI(value);

                        if (uri.getScheme() == null || !List.of("http", "https").contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
                            return String.format(
                                    "dataset.xml: protocol '%s' in uri '%s' is not one of the accepted protocols [http, https]", uri.getScheme(), uri
                            );
                        }
                    }
                    catch (URISyntaxException e) {
                        return String.format("dataset.xml: '%s' is not a valid uri", value);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        log.debug("Invalid URI's found: {}", errors);

        if (errors.size() > 0) {
            return RuleResult.error(errors);
        }

        return RuleResult.ok();

    }
}
