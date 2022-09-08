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
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.lib.dataverse.model.search.DatasetResultItem;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
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

    public DatastationRulesImpl(BagItMetadataReader bagItMetadataReader, DataverseService dataverseService) {
        this.bagItMetadataReader = bagItMetadataReader;
        this.dataverseService = dataverseService;
    }

    @Override
    public BagValidatorRule organizationalIdentifierIsValid() {

        // TODO do not check with dataverse for is-version-of,
        // just check if Is-Version-Of is set if Has-Org... is defined and a
        // dataset exists in dataverse
        // TODO wait for Jan's thoughts
        return path -> {
            var hasOrganizationalIdentifier = bagItMetadataReader.getField(path, "Has-Organizational-Identifier");

            if (hasOrganizationalIdentifier.isEmpty()) {
                return RuleResult.skipDependencies();
            }

            else if (hasOrganizationalIdentifier.size() > 1) {
                return RuleResult.error("More than one 'Has-Organizational-Identifier' field found in bag, only one allowed");
            }

            var identifier = hasOrganizationalIdentifier.get(0);
            // get isVersionOf or null if it doesn't exist
            // note that the limitation of 'there can only be one' is checked in another rule
            var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");

            // check if dataset exists in the datastation
            var searchResult = dataverseService.searchDatasetsByOrganizationalIdentifier(identifier);
            var searchResultItems = searchResult.getData().getItems().stream()
                .filter(s -> s instanceof DatasetResultItem)
                .map(s -> (DatasetResultItem) s)
                .collect(Collectors.toList());

            // the rule only applies if there is a matching document
            if (!searchResultItems.isEmpty()) {
                var deposit = searchResultItems.get(0);

                // when the dataset is found in dataverse, is-version-of is mandatory
                if (isVersionOf == null) {
                    return RuleResult.error("'Is-Version-Of' is required when 'Has-Organizational-Identifier' is set and there is a matching dataset in Dataverse");
                }

                var dataset = dataverseService.getDataset(deposit.getGlobalId());

                // get the dansBagId value from the metadata blocks using the optional and streaming capabilities
                var dansBagId = Optional.ofNullable(dataset.getData().getLatestVersion().getMetadataBlocks())
                    .map(m -> m.get("dansDataVaultMetadata"))
                    .map(m -> m.getFields().stream()
                        .filter(f -> f.getTypeName().equals("dansSwordToken"))
                        .filter(f -> f instanceof PrimitiveSingleValueField)
                        .map(f -> (PrimitiveSingleValueField) f)
                        .map(PrimitiveSingleValueField::getValue)
                        .findFirst()
                    )
                    .flatMap(f -> f)
                    .orElse(null);

                if (!isVersionOf.equals(dansBagId)) {
                    return RuleResult.error(String.format(
                        "'Is-Version-Of' (%s) does not refer to the same bag as the 'Has-Organizational-Identifier' (%s) would expect, but instead refers to bag with ID %s",
                        isVersionOf, identifier, dansBagId
                    ));
                }
            }

            return RuleResult.ok();
        };
    }

    @Override
    public BagValidatorRule isVersionOfIsAValidSwordToken() {
        return path -> {
            var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");

            if (isVersionOf != null) {
                var result = dataverseService.searchBySwordToken(isVersionOf)
                    .getData().getItems().stream()
                    .filter(resultItem -> resultItem instanceof DatasetResultItem)
                    .map(resultItem -> (DatasetResultItem) resultItem)
                    .collect(Collectors.toList());

                // no result means it does not exist
                if (result.isEmpty()) {
                    return RuleResult.error(String.format(
                        "If 'Is-Version-Of' is specified, it must be a valid SWORD token in the data station; no tokens were found: %s", isVersionOf
                    ));
                }
                else {
                    // (b) dansOtherId must match Has-Organizational-Identifier (or both are null)
                    var orgIdentifier = bagItMetadataReader.getSingleField(path, "Has-Organizational-Identifier");

                    var matchingDatasets = result.stream()
                        .filter(searchResult -> {
                            try {
                                return isValidOrganizationalIdentifierDataset(orgIdentifier, searchResult.getGlobalId());
                            }
                            catch (IOException | DataverseException e) {
                                log.error("Error while searching for dataset", e);
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

                    if (matchingDatasets.isEmpty()) {
                        return RuleResult.error(String.format("No datasets found that have the same value as 'Has-Organizational-Identifier' (%s)", orgIdentifier));
                    }
                    else {
                        // (c) the user in data-station-user-account is authorized
                        var username = bagItMetadataReader.getSingleField(path, "Data-Station-User_account");

                        if (username != null) {
                            var authorizedDatasets = matchingDatasets.stream()
                                .filter(datasetResultItem -> {
                                    try {
                                        return userIsAuthorizedToUpdateDataset(username, datasetResultItem.getGlobalId());
                                    }
                                    catch (IOException | DataverseException e) {
                                        log.error("Error checking dataset authorization", e);
                                    }
                                    return false;
                                })
                                .collect(Collectors.toList());

                            if (authorizedDatasets.isEmpty()) {
                                return RuleResult.error(String.format("No datasets found that user %s is authorized to update", username));
                            }
                        }
                        else {
                            log.debug("No username found in metadata, not checking with dataverse");
                        }
                    }

                    return RuleResult.ok();
                }
            }

            return RuleResult.skipDependencies();
        };
    }

    @Override
    public BagValidatorRule dataStationUserAccountIsAuthorized() {
        return path -> {
            var userAccount = bagItMetadataReader.getSingleField(path, "Data-Station-User-Account");
            var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");

            if (userAccount != null && isVersionOf != null) {
                // get all matching datasets, and then convert them to DatasetResultItems
                var result = dataverseService.searchBySwordToken(isVersionOf)
                    .getData().getItems().stream()
                    .filter(resultItem -> resultItem instanceof DatasetResultItem)
                    .map(resultItem -> (DatasetResultItem) resultItem)
                    .collect(Collectors.toList());

                // no result means it does not exist
                if (result.isEmpty()) {
                    return RuleResult.error(String.format(
                        "If 'Is-Version-Of' is specified, it must be a valid SWORD token in the data station; no tokens were found: %s", isVersionOf
                    ));
                }

                var itemId = result.get(0).getGlobalId();

                var assignments = dataverseService.getRoleAssignments(itemId);

                // when the user has one of these valid roles, the check succeeds
                var validRoles = dataverseService.getAllowedDepositorRoles();
                var matchingAssignments = assignments.getData().stream()
                    .filter(a -> a.getAssignee().replaceFirst("@", "").equals(userAccount))
                    .filter(a -> validRoles.contains(a.get_roleAlias()))
                    .collect(Collectors.toList());

                if (matchingAssignments.isEmpty()) {
                    return RuleResult.error(String.format(
                        "If 'Data-Station-User-Account' is set, it must contain the user name of the Data Station account that is authorized to deposit the bag with ID %s; user name is %s",
                        isVersionOf, userAccount
                    ));
                }

                return RuleResult.ok();
            }

            return RuleResult.skipDependencies();
        };

    }

    boolean isValidOrganizationalIdentifierDataset(String orgIdentifier, String datasetId) throws IOException, DataverseException {

        var dataset = dataverseService.getDataset(datasetId);
        var otherId = Optional.ofNullable(dataset.getData().getLatestVersion().getMetadataBlocks())
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

        if (otherId == null && orgIdentifier == null) {
            log.trace("Dataset does not have 'dansOtherId' set, nor is 'Has-Organizational-Identifier' set in the bag information, so this is a valid match");
            return true;
        }
        else if (Objects.equals(otherId, orgIdentifier)) {
            // this is also valid
            log.trace("Dataset with dansOtherId {} and 'Has-Organizational-Identifier' {} match", otherId, orgIdentifier);
            return true;
        }
        else {
            // this is not valid
            log.trace("Dataset with dansOtherId {} and 'Has-Organizational-Identifier' {} do not match", otherId, orgIdentifier);
        }
        return false;
    }

    boolean userIsAuthorizedToUpdateDataset(String user, String datasetId) throws IOException, DataverseException {
        var assignments = dataverseService.getRoleAssignments(datasetId);

        // when the user has one of these valid roles, the check succeeds
        var validRoles = dataverseService.getAllowedDepositorRoles();
        var matchingAssignments = assignments.getData().stream()
            .filter(a -> a.getAssignee().replaceFirst("@", "").equals(user))
            .filter(a -> validRoles.contains(a.get_roleAlias()))
            .collect(Collectors.toList());

        return !matchingAssignments.isEmpty();
    }
}
