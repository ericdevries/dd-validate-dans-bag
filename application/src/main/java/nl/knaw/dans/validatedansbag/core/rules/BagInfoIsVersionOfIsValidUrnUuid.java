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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class BagInfoIsVersionOfIsValidUrnUuid implements BagValidatorRule {
    private final BagItMetadataReader bagItMetadataReader;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var items = bagItMetadataReader.getField(path, "Is-Version-Of");

        var invalidUrns = items.stream().filter(item -> {
                log.debug("Validating if {} is a valid URN UUID ", item);

                try {
                    var uri = new URI(item);

                    if (!"urn".equalsIgnoreCase(uri.getScheme())) {
                        log.debug("{} is not the expected value 'urn'", uri.getScheme());
                        return true;
                    }

                    if (!uri.getSchemeSpecificPart().startsWith("uuid:")) {
                        log.debug("{} does not start with 'uuid:'", uri.getSchemeSpecificPart());
                        return true;
                    }

                    //noinspection ResultOfMethodCallIgnored
                    UUID.fromString(uri.getSchemeSpecificPart().substring("uuid:".length()));
                }
                catch (URISyntaxException | IllegalArgumentException e) {
                    log.trace("{} could not be parsed", item, e);
                    return true;
                }

                return false;
            })
            .collect(Collectors.toList());

        log.debug("Invalid URN UUID's from this list ({}) are {}", items, invalidUrns);

        if (!invalidUrns.isEmpty()) {
            return RuleResult.error(
                String.format("bag-info.txt Is-Version-Of value must be a valid URN: Invalid items {%s}", String.join(", ", invalidUrns))
            );
        }

        return RuleResult.ok();
    }
}
