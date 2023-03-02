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

import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetLatestVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.lib.dataverse.model.search.DatasetResultItem;
import nl.knaw.dans.validatedansbag.core.config.SwordDepositorRoles;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DatastationRulesImpl implements DatastationRules {
    private static final Logger log = LoggerFactory.getLogger(DatastationRulesImpl.class);
    private final BagItMetadataReader bagItMetadataReader;
    private final DataverseService dataverseService;
    private final SwordDepositorRoles swordDepositorRoles;
    private final XmlReader xmlReader;

    public DatastationRulesImpl(BagItMetadataReader bagItMetadataReader, DataverseService dataverseService, SwordDepositorRoles swordDepositorRoles, XmlReader xmlReader) {
        this.bagItMetadataReader = bagItMetadataReader;
        this.dataverseService = dataverseService;
        this.swordDepositorRoles = swordDepositorRoles;
        this.xmlReader = xmlReader;
    }

    @Override
    public BagValidatorRule bagExistsInDatastation() {

        return (path) -> {
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
                }
                else {
                    log.debug("Dataset with sword token '{}': {}", isVersionOf, dataset.get());
                    return RuleResult.ok();
                }
            }

            return RuleResult.skipDependencies();
        };
    }

    @Override
    public BagValidatorRule organizationalIdentifierExistsInDataset() {
        return path -> {
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
            }
            else if (Objects.equals(otherId, orgIdentifier)) {
                // this is also valid
                log.trace("Dataset with dansOtherId {} and 'Has-Organizational-Identifier' {} match", otherId, orgIdentifier);
                return RuleResult.ok();
            }
            else {
                // this is not valid
                log.trace("Dataset with dansOtherId {} and 'Has-Organizational-Identifier' {} do not match", otherId, orgIdentifier);
                return RuleResult.error(String.format(
                    "Mismatch between 'dansOtherId' in dataverse and 'Has-Organizational-Identifier' in dataset: '%s' vs '%s'. They must either both be the same or both be absent",
                    orgIdentifier, otherId
                ));
            }
        };
    }

    @Override
    public BagValidatorRule embargoPeriodWithinLimits() {
        return (path) -> {
            var months = Integer.parseInt(dataverseService.getMaxEmbargoDurationInMonths().getData().getMessage());
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
            var expr = "/ddm:DDM/ddm:profile/ddm:available";

            var nodes = xmlReader.xpathToStream(document, expr).collect(Collectors.toList());
            if (nodes.isEmpty()) {
                return RuleResult.ok();
            }

            DateTime embargoDate = DateTime.parse(nodes.get(0).getTextContent());
            if (embargoDate.isBefore(new DateTime(DateTime.now().plusMonths(months)))) {
                return RuleResult.ok();
            }
            else {
                return RuleResult.error("Date available is further is the future than the Embargo Period allows");
            }
        };
    }

    Optional<DatasetLatestVersion> getDatasetIsVersionOf(String isVersionOf) throws IOException, DataverseException {
        if (isVersionOf.startsWith("urn:uuid:")) {
            var swordToken = "sword:" + isVersionOf.substring("urn:uuid:".length());
            var result = dataverseService.searchBySwordToken(swordToken)
                .getData().getItems().stream()
                .filter(resultItem -> resultItem instanceof DatasetResultItem)
                .map(resultItem -> (DatasetResultItem) resultItem)
                .findFirst();
            if (result.isPresent()) {
                var globalId = result.get().getGlobalId();
                return Optional.ofNullable(dataverseService.getDataset(globalId).getData());
            }

            return Optional.empty();
        }
        else {
            throw new IllegalArgumentException("Is-Version-Of is not a urn:uuid");
        }
    }
}
