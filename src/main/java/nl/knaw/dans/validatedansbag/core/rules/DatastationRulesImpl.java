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
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DatastationRulesImpl implements DatastationRules {
    private static final Logger log = LoggerFactory.getLogger(DatastationRulesImpl.class);
    private final BagItMetadataReader bagItMetadataReader;
    private final DataverseService dataverseService;

    public DatastationRulesImpl(BagItMetadataReader bagItMetadataReader, DataverseService dataverseService) {
        this.bagItMetadataReader = bagItMetadataReader;
        this.dataverseService = dataverseService;
    }

    @Override
    public BagValidatorRule bagExistsInDatastation() {

        return (path) -> {
            var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");

            if (isVersionOf != null) {
                var dataset = getDatasetBySwordToken(isVersionOf);

                if (dataset.isEmpty()) {
                    // no result means it does not exist
                    return RuleResult.error(String.format(
                        "If 'Is-Version-Of' is specified, it must be a valid SWORD token in the data station; no tokens were found: %s", isVersionOf
                    ));
                }
                else {
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
            var dataset = getDatasetBySwordToken(isVersionOf);

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

                var userRoles = result.stream()
                    .filter(a -> a.getAssignee().replaceFirst("@", "").equals(userAccount))
                    .map(RoleAssignmentReadOnly::get_roleAlias)
                    .collect(Collectors.toList());

                var validRole = dataverseService.getAllowedCreatorRole();

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

            // both userAccount and isVersionOf are required fields at this point, but they are checked in other steps
            // so to keep this rule oblivious of other requirements, just check if we have values
            if (userAccount != null && isVersionOf != null) {
                var dataset = getDatasetBySwordToken(isVersionOf);

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

                // when the user has one of these valid roles, the check succeeds
                var validRole = dataverseService.getAllowedEditorRole();

                // get all roles assigned to this user
                var assignmentsNames = assignments
                    .stream()
                    .filter(a -> a.getAssignee().replaceFirst("@", "").equals(userAccount))
                    .map(RoleAssignmentReadOnly::get_roleAlias)
                    .collect(Collectors.toList());

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

    Optional<DatasetLatestVersion> getDatasetBySwordToken(String swordToken) throws IOException, DataverseException {
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
}
