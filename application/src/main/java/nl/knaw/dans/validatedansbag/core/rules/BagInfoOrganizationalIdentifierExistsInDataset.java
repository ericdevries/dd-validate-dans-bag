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
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class BagInfoOrganizationalIdentifierExistsInDataset extends DataverseRuleBase implements BagValidatorRule {
    private final BagItMetadataReader bagItMetadataReader;

    public BagInfoOrganizationalIdentifierExistsInDataset(DataverseService dataverseService, BagItMetadataReader bagItMetadataReader) {
        super(dataverseService);
        this.bagItMetadataReader = bagItMetadataReader;
    }

    @Override
    public RuleResult validate(Path path) throws Exception {
        var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");
        var dataset = getDatasetIsVersionOf(isVersionOf);

        if (dataset.isEmpty()) {
            return RuleResult.error("Expected a dataset, but got nothing");
        }

        // (b) dansOtherId must match Has-Organizational-Identifier (or both are null)
        var orgIdentifier = bagItMetadataReader.getSingleField(path, "Has-Organizational-Identifier");

        var otherId = Optional.ofNullable(dataset.get().getLatestVersion().getMetadataBlocks())
                .map(m -> m.get("dansDataVaultMetadata"))
                .map(m -> m.getFields().stream()
                        .filter(f -> f.getTypeName().equals("dansOtherId"))
                        .filter(f -> f instanceof PrimitiveSingleValueField)
                        .map(f -> (PrimitiveSingleValueField) f)
                        .map(PrimitiveSingleValueField::getValue)
                        .findFirst()
                )
                .flatMap(f -> f)
                .orElse(null);

        if ("".equals(otherId) || "null".equals(otherId)) {
            otherId = null;
        }

        if (otherId == null && orgIdentifier == null) {
            log.trace("Dataset does not have 'dansOtherId' set, nor is 'Has-Organizational-Identifier' set in the bag information, so this is a valid match");
            return RuleResult.ok();
        } else if (Objects.equals(otherId, orgIdentifier)) {
            // this is also valid
            log.trace("Dataset with dansOtherId {} and 'Has-Organizational-Identifier' {} match", otherId, orgIdentifier);
            return RuleResult.ok();
        } else {
            // this is not valid
            log.trace("Dataset with dansOtherId {} and 'Has-Organizational-Identifier' {} do not match", otherId, orgIdentifier);
            return RuleResult.error(String.format(
                    "Mismatch between 'dansOtherId' in dataverse and 'Has-Organizational-Identifier' in dataset: '%s' vs '%s'. They must either both be the same or both be absent",
                    orgIdentifier, otherId
            ));
        }
    }
}
