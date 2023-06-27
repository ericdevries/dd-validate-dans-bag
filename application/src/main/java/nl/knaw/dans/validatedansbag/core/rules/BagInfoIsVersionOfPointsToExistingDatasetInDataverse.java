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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;

import java.nio.file.Path;

@Slf4j
public class BagInfoIsVersionOfPointsToExistingDatasetInDataverse extends DataverseRuleBase implements BagValidatorRule {
    private final BagItMetadataReader bagItMetadataReader;

    public BagInfoIsVersionOfPointsToExistingDatasetInDataverse(DataverseService dataverseService, BagItMetadataReader bagItMetadataReader) {
        super(dataverseService);
        this.bagItMetadataReader = bagItMetadataReader;
    }


    @Override
    public RuleResult validate(Path path) throws Exception {
        var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");

        log.trace("Using Is-Version-Of value '{}' to find a matching dataset", isVersionOf);

        if (isVersionOf != null) {
            var dataset = getDatasetIsVersionOf(isVersionOf);

            if (dataset.isEmpty()) {
                log.debug("Dataset with sword token '{}' not found", isVersionOf);
                // no result means it does not exist
                return RuleResult.error(String.format(
                        "If 'Is-Version-Of' is specified, it must be a valid SWORD token in the data station; no tokens were found: %s", isVersionOf
                ));
            } else {
                log.debug("Dataset with sword token '{}': {}", isVersionOf, dataset.get());
                return RuleResult.ok();
            }
        }

        return RuleResult.skipDependencies();
    }
}
