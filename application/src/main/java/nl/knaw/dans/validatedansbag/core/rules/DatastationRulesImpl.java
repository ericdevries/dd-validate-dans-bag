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
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetLatestVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.lib.dataverse.model.search.DatasetResultItem;
import nl.knaw.dans.validatedansbag.core.config.SwordDepositorRoles;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidator;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DatastationRulesImpl implements DatastationRules {
    private static final Logger log = LoggerFactory.getLogger(DatastationRulesImpl.class);
    private final BagItMetadataReader bagItMetadataReader;
    private final DataverseService dataverseService;
    private final SwordDepositorRoles swordDepositorRoles;
    private final XmlReader xmlReader;
    private final LicenseValidator licenseValidator;

    public DatastationRulesImpl(BagItMetadataReader bagItMetadataReader, DataverseService dataverseService, SwordDepositorRoles swordDepositorRoles, XmlReader xmlReader,
        LicenseValidator licenseValidator) {
        this.bagItMetadataReader = bagItMetadataReader;
        this.dataverseService = dataverseService;
        this.swordDepositorRoles = swordDepositorRoles;
        this.xmlReader = xmlReader;
        this.licenseValidator = licenseValidator;
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
    public BagValidatorRule userIsAuthorizedToCreateDataset() {
        return path -> {
            var userAccount = bagItMetadataReader.getSingleField(path, "Data-Station-User-Account");

            if (userAccount != null) {
                var result = Optional.ofNullable(dataverseService.getDataverseRoleAssignments("root"))
                    .map(d -> {
                        try {
                            return d.getData();
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .orElse(List.of());

                log.debug("Role assignments found in dataverse: {}", result);

                var userRoles = result.stream()
                    .filter(a -> a.getAssignee().replaceFirst("@", "").equals(userAccount))
                    .map(RoleAssignmentReadOnly::get_roleAlias)
                    .collect(Collectors.toList());

                var validRole = swordDepositorRoles.getDatasetCreator();

                if (!userRoles.contains(validRole)) {
                    return RuleResult.error(String.format(
                        "User '%s' does not have the correct role for creating datasets (expected: %s, found: %s)",
                        userAccount, validRole, userRoles
                    ));
                }

                return RuleResult.ok();
            }

            return RuleResult.skipDependencies();
        };

    }

    @Override
    public BagValidatorRule userIsAuthorizedToUpdateDataset() {
        return path -> {
            var userAccount = bagItMetadataReader.getSingleField(path, "Data-Station-User-Account");
            var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");

            log.debug("Checking if user '{}' is authorized on dataset '{}'", userAccount, isVersionOf);
            // both userAccount and isVersionOf are required fields at this point, but they are checked in other steps
            // so to keep this rule oblivious of other requirements, just check if we have values
            if (userAccount != null && isVersionOf != null) {
                var dataset = getDatasetIsVersionOf(isVersionOf);

                // no result means it does not exist
                if (dataset.isEmpty()) {
                    return RuleResult.error(String.format(
                        "If 'Is-Version-Of' is specified, it must be a valid SWORD token in the data station; no tokens were found: %s", isVersionOf
                    ));
                }

                var itemId = dataset.get().getLatestVersion().getDatasetPersistentId();
                var assignments = Optional.ofNullable(dataverseService.getDatasetRoleAssignments(itemId))
                    .map(d -> {
                        try {
                            return d.getData();
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .orElse(List.of());

                log.debug("Role assignments on dataset: {}", assignments);

                // when the user has one of these valid roles, the check succeeds
                var validRole = swordDepositorRoles.getDatasetEditor();

                // get all roles assigned to this user
                var assignmentsNames = assignments
                    .stream()
                    .filter(a -> a.getAssignee().replaceFirst("@", "").equals(userAccount))
                    .map(RoleAssignmentReadOnly::get_roleAlias)
                    .collect(Collectors.toList());

                log.debug("Role assignments for user '{}': {}", userAccount, assignmentsNames);

                if (!assignmentsNames.contains(validRole)) {
                    return RuleResult.error(String.format(
                        "User '%s' does not have the correct role for creating datasets (expected: %s, found: %s)",
                        userAccount, validRole, assignmentsNames
                    ));
                }

                return RuleResult.ok();
            }

            return RuleResult.skipDependencies();
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

    @Override
    public BagValidatorRule licenseExistsInDatastation() {
        return path -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
            // converts a namespace uri into a prefix that is used in the document
            var prefix = document.lookupPrefix(XmlReader.NAMESPACE_DCTERMS);
            var expr = String.format("/ddm:DDM/ddm:dcmiMetadata/dcterms:license[@xsi:type='%s:URI']", prefix);

            var validNodes = xmlReader.xpathToStream(document, expr)
                .filter(item -> licenseValidator.isValidLicenseURI(item.getTextContent()))
                .collect(Collectors.toList());

            log.debug("Nodes found with valid URI's: {}", validNodes.size());

            var invalidLicenses = new ArrayList<String>();

            for (var node : validNodes) {
                var isValid = false;
                var text = node.getTextContent();

                log.debug("Validating if {} is a valid license in data station", text);
                try {
                    isValid = licenseValidator.isValidLicense(text);
                }
                catch (IOException | DataverseException e) {
                    log.error("Unable to validate licenses with dataverse", e);
                }

                if (!isValid) {
                    invalidLicenses.add(text);
                }
            }

            if (invalidLicenses.size() > 0) {
                return RuleResult.error(String.format(
                    "Invalid licenses found that are not available in the data station: %s", invalidLicenses
                ));
            }

            return RuleResult.ok();
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
