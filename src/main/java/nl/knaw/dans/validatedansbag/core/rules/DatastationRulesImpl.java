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

import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.lib.dataverse.model.search.DatasetResultItem;
import nl.knaw.dans.validatedansbag.core.engine.RuleSkippedException;
import nl.knaw.dans.validatedansbag.core.engine.RuleViolationDetailsException;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DatastationRulesImpl implements DatastationRules {
    private final BagItMetadataReader bagItMetadataReader;
    private final DataverseService dataverseService;

    public DatastationRulesImpl(BagItMetadataReader bagItMetadataReader, DataverseService dataverseService) {
        this.bagItMetadataReader = bagItMetadataReader;
        this.dataverseService = dataverseService;
    }

    /**
     * (a) The bag-info.txt file MAY contain at most one element called Has-Organizational-Identifier. The value of this element MUST start with a client specific prefix agreed between the client and
     * DANS. If there already exists a dataset in the targeted Data Station with the same value in the dataset metadata field dansOtherId then Is-Version-Of as described in 1.2.4 MUST be specified to
     * indicate that this deposit is an update of that dataset. (b) If this Has-Organizational-Identifier is given, at most one Has-Organizational-Identifier-Version MAY be present, containing the
     * version number.
     *
     * @return
     */
    @Override
    public BagValidatorRule organizationalIdentifierIsValid() {

        // TODO do not check with dataverse for is-version-of,
        // just check if Is-Version-Of is set if Has-Org... is defined and a
        // dataset exists in dataverse
        // TODO wait for Jan's thoughts
        return path -> {
            var hasOrganizationalIdentifier = bagItMetadataReader.getField(path, "Has-Organizational-Identifier");

            if (hasOrganizationalIdentifier.isEmpty()) {
                throw new RuleSkippedException();
            }

            else if (hasOrganizationalIdentifier.size() > 1) {
                throw new RuleViolationDetailsException("More than one 'Has-Organizational-Identifier' field found in bag, only one allowed");
            }

            var identifier = hasOrganizationalIdentifier.get(0);
            // get isVersionOf or null if it doesn't exist
            // note that the limitation of 'there can only be one' is checked in another rule
            var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");

            // check if prefix matches what was agreed on
            // TODO determine how to check for a client specific prefix agreed between the client and DANS

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
                    throw new RuleViolationDetailsException("'Is-Version-Of' is required when 'Has-Organizational-Identifier' is set and there is a matching dataset in Dataverse");
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
                    throw new RuleViolationDetailsException(String.format(
                        "'Is-Version-Of' (%s) does not refer to the same bag as the 'Has-Organizational-Identifier' (%s) would expect, but instead refers to bag with ID %s",
                        isVersionOf, identifier, dansBagId
                    ));
                }
            }
        };
    }

    @Override
    public BagValidatorRule isVersionOfIsAValidSwordToken() {
        return path -> {
            var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");

            if (isVersionOf != null) {
                var result = dataverseService.searchBySwordToken(isVersionOf);
                var data = result.getData().getItems();

                // no result means it does not exist
                if (data.isEmpty()) {
                    throw new RuleViolationDetailsException(String.format(
                        "If 'Is-Version-Of' is specified, it must be a valid SWORD token in the data station; no tokens were found: %s", isVersionOf
                    ));
                }
            }
        };
    }

    @Override
    public BagValidatorRule dataStationUserAccountIsAuthorized() {
        return path -> {
            var userAccount = bagItMetadataReader.getSingleField(path, "Data-Station-User-Account");
            var isVersionOf = bagItMetadataReader.getSingleField(path, "Is-Version-Of");

            if (userAccount != null && isVersionOf != null) {
                var result = dataverseService.searchBySwordToken(isVersionOf);
                var data = result.getData().getItems();

                // no result means it does not exist
                if (data.isEmpty()) {
                    throw new RuleViolationDetailsException(String.format(
                        "If 'Is-Version-Of' is specified, it must be a valid SWORD token in the data station; no tokens were found: %s", isVersionOf
                    ));
                }

                var item = (DatasetResultItem) data.get(0);
                var itemId = item.getGlobalId();

                var assignments = dataverseService.getRoleAssignments(itemId);

                // TODO move to config?
                var validRoles = Set.of("contributor", "contributorplus");
                var matchingAssignments = assignments.getData().stream()
                    .filter(a -> a.getAssignee().replaceFirst("@", "").equals(userAccount))
                    .filter(a -> validRoles.contains(a.get_roleAlias()))
                    .collect(Collectors.toList());

                if (matchingAssignments.isEmpty()) {
                    throw new RuleViolationDetailsException(String.format(
                        "If 'Data-Station-User-Account' is set, it must contain the user name of the Data Station account that is authorized to deposit the bag with ID %s; user name is %s",
                        isVersionOf, userAccount
                    ));
                }
            }
        };
    }
}
