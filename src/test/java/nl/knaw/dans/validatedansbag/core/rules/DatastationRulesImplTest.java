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
import nl.knaw.dans.lib.dataverse.model.search.SearchResult;
import nl.knaw.dans.validatedansbag.core.config.SwordDepositorRoles;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.resources.MockedDataverseResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatastationRulesImplTest {

    final BagItMetadataReader bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
    final DataverseService dataverseService = Mockito.mock(DataverseService.class);
    final SwordDepositorRoles swordDepositorRoles = new SwordDepositorRoles("datasetcreator", "dataseteditor");

    DatastationRulesImplTest() {
    }

    @AfterEach
    void afterEach() {
        Mockito.reset(bagItMetadataReader);
        Mockito.reset(dataverseService);
    }

    @Test
    void bagExistsInDatastation() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito.doReturn("is-version-of-id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, null));

        var result = checker.bagExistsInDatastation().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void bagNotExistsInDatastation() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito.doReturn("is-version-of-id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        String emptySearchResult = getEmptySearchResult();
        mockSearchBySwordToken(emptySearchResult);

        var result = checker.bagExistsInDatastation().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDataset() throws Exception {

        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        String otherId = "dans-other-id";
        Mockito.doReturn(otherId)
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, otherId));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDatasetBothAreNull() throws Exception {

        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito.doReturn(null)
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, null));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDatasetActualIsNull() throws Exception {

        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.anyString()))
            .thenReturn("is_version_of")
            .thenReturn("has_organizational_identifier");

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, null));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDatasetMismatch() throws Exception {

        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.anyString()))
            .thenReturn("is_version_of")
            .thenReturn("has_organizational_identifier");

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, "some_other_organizational_identifier"));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    // CREATE tests
    @Test
    void dataStationUserAccountIsAuthorizedToCreate() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito
            .doReturn("user-account-name")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@user-account-name\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"datasetcreator\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 5,\n"
            + "      \"assignee\": \"@datamanager001\",\n"
            + "      \"roleId\": 9,\n"
            + "      \"_roleAlias\": \"datamanager\",\n"
            + "      \"definitionPointId\": 1\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        mockGetDataverseRoleAssignments(assignmentResult);

        var result = checker.userIsAuthorizedToCreateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void dataStationUserAccountIsNotAuthorizedToCreate() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito
            .doReturn("user-account-name")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@dataverseAdmin\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"dataseteditor\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@user-account-name\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"differentrole\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 5,\n"
            + "      \"assignee\": \"@datamanager001\",\n"
            + "      \"roleId\": 9,\n"
            + "      \"_roleAlias\": \"datamanager\",\n"
            + "      \"definitionPointId\": 1\n"
            + "    }\n"
            + "  ]\n"
            + "}";

        mockGetDataverseRoleAssignments(assignmentResult);

        var result = checker.userIsAuthorizedToCreateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void dataStationUserAccountIsNotSetCreate() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito
            .doReturn(null)
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@dataverseAdmin\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"contributorplus\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 5,\n"
            + "      \"assignee\": \"@datamanager001\",\n"
            + "      \"roleId\": 9,\n"
            + "      \"_roleAlias\": \"datamanager\",\n"
            + "      \"definitionPointId\": 1\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        mockGetDataverseRoleAssignments(assignmentResult);

        var result = checker.userIsAuthorizedToCreateDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SKIP_DEPENDENCIES, result.getStatus());
    }

    @Test
    void dataStationUserAccountYieldsNoSearchResultsCreate() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito
            .doReturn("user-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@dataverseAdmin\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"contributorplus\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 5,\n"
            + "      \"assignee\": \"@datamanager001\",\n"
            + "      \"roleId\": 9,\n"
            + "      \"_roleAlias\": \"datamanager\",\n"
            + "      \"definitionPointId\": 1\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        mockGetDataverseRoleAssignments(assignmentResult);

        var result = checker.userIsAuthorizedToCreateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    // EDIT tests

    @Test
    void dataStationUserAccountIsAuthorizedToEdit() throws Exception {
        var dv = dataverseService;
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv, swordDepositorRoles);

        Mockito
            .doReturn("user-account-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@user-account-name\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"dataseteditor\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 5,\n"
            + "      \"assignee\": \"@datamanager001\",\n"
            + "      \"roleId\": 9,\n"
            + "      \"_roleAlias\": \"datamanager\",\n"
            + "      \"definitionPointId\": 1\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        mockSearchBySwordToken(getSearchResult("doi"));
        mockGetDataset(null);
        mockGetDataset(getLatestVersion("doi", "otherid"));
        mockGetDatasetRoleAssignments(assignmentResult);

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());

        Mockito.verify(dv).getDataset(Mockito.eq("doi"));
    }

    @Test
    void dataStationUserAccountIsNotAuthorizedToEdit() throws Exception {
        var dv = dataverseService;
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv, swordDepositorRoles);

        Mockito
            .doReturn("user-account-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@dataverseAdmin\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"dataseteditor\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 5,\n"
            + "      \"assignee\": \"@datamanager001\",\n"
            + "      \"roleId\": 9,\n"
            + "      \"_roleAlias\": \"datamanager\",\n"
            + "      \"definitionPointId\": 1\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        mockSearchBySwordToken(getSearchResult("doi"));
        mockGetDataset(null);
        mockGetDataset(getLatestVersion("doi", "otherid"));
        mockGetDatasetRoleAssignments(assignmentResult);

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        Mockito.verify(dv).getDataset(Mockito.eq("doi"));
    }

    @Test
    void dataStationUserAccountIsNotAuthorizedToEditButHasADifferentRole() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito
            .doReturn("user-account-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@user-account-name\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"readonly\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 5,\n"
            + "      \"assignee\": \"@datamanager001\",\n"
            + "      \"roleId\": 9,\n"
            + "      \"_roleAlias\": \"datamanager\",\n"
            + "      \"definitionPointId\": 1\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        mockSearchBySwordToken(getSearchResult("doi"));
        mockGetDataset(getLatestVersion("doi", "dans-other-id"));
        mockGetDatasetRoleAssignments(assignmentResult);

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        String firstMsg = result.getErrorMessages().get(0);
        assertTrue(firstMsg.contains("user-account-name"), firstMsg);
        assertTrue(firstMsg.contains("expected: dataseteditor"), firstMsg);
        assertTrue(firstMsg.contains("readonly"), firstMsg);
    }

    @Test
    void dataStationUserAccountIsNotSetEdit() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito
            .doReturn(null)
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@dataverseAdmin\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"contributorplus\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 5,\n"
            + "      \"assignee\": \"@datamanager001\",\n"
            + "      \"roleId\": 9,\n"
            + "      \"_roleAlias\": \"datamanager\",\n"
            + "      \"definitionPointId\": 1\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        mockSearchBySwordToken(getEmptySearchResult());
        mockSearchBySwordToken(getSearchResult("doi"));
        mockGetDataverseRoleAssignments(assignmentResult);

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SKIP_DEPENDENCIES, result.getStatus());
    }

    @Test
    void dataStationUserAccountYieldsNoSearchResultsEdit() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles);

        Mockito
            .doReturn("user-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@dataverseAdmin\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"contributorplus\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": 5,\n"
            + "      \"assignee\": \"@datamanager001\",\n"
            + "      \"roleId\": 9,\n"
            + "      \"_roleAlias\": \"datamanager\",\n"
            + "      \"definitionPointId\": 1\n"
            + "    }\n"
            + "  ]\n"
            + "}";

        mockSearchBySwordToken(getEmptySearchResult());
        mockGetDataset("doi");
        mockGetDataverseRoleAssignments(assignmentResult);

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));

        assertTrue(result.getErrorMessages().get(0).contains("it must be a valid SWORD token in the data station"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    private void mockGetDataset(String json) throws IOException, DataverseException {
        var response = new MockedDataverseResponse<>(json, DatasetLatestVersion.class);
        Mockito.doReturn(response).when(dataverseService).getDataset(Mockito.anyString());
    }

    private void mockSearchBySwordToken(String json) throws IOException, DataverseException {
        var response = new MockedDataverseResponse<>(json, SearchResult.class);
        Mockito.doReturn(response).when(dataverseService).searchBySwordToken(Mockito.any());
    }

    private void mockGetDataverseRoleAssignments(String json) throws IOException, DataverseException {
        var response = new MockedDataverseResponse<>(json, List.class, RoleAssignmentReadOnly.class);
        Mockito.doReturn(response).when(dataverseService).getDataverseRoleAssignments(Mockito.any());
    }

    private void mockGetDatasetRoleAssignments(String json) throws IOException, DataverseException {
        var response = new MockedDataverseResponse<>(json, List.class, RoleAssignmentReadOnly.class);
        Mockito.doReturn(response).when(dataverseService).getDatasetRoleAssignments(Mockito.any());
    }

    String getSearchResult(String globalId) {
        return String.format("{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"q\": \"NBN:urn:nbn:nl:ui:13-025de6e2-bdcf-4622-b134-282b4c590f42\",\n"
            + "    \"total_count\": 1,\n"
            + "    \"start\": 0,\n"
            + "    \"spelling_alternatives\": {},\n"
            + "    \"items\": [\n"
            + "      {\n"
            + "        \"name\": \"Manual Test\",\n"
            + "        \"type\": \"dataset\",\n"
            + "        \"url\": \"https://doi.org/10.5072/FK2/QZZSST\",\n"
            + "        \"global_id\": \"%s\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"count_in_response\": 1\n"
            + "  }\n"
            + "}", globalId);

    }

    String getEmptySearchResult() {
        return "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"q\": \"NBN:urn:nbn:nl:ui:13-025de6e2-bdcf-4622-b134-282b4c590f42\",\n"
            + "    \"total_count\": 1,\n"
            + "    \"start\": 0,\n"
            + "    \"spelling_alternatives\": {},\n"
            + "    \"items\": [\n"
            + "    ],\n"
            + "    \"count_in_response\": 0\n"
            + "  }\n"
            + "}";
    }

    String getLatestVersion(String persistentId, String dansOtherId) {

        if (persistentId == null) {
            persistentId = "persistent_id";
        }
        if (dansOtherId == null) {
            dansOtherId = "null";
        }
        else {
            dansOtherId = "\"" + dansOtherId + "\"";
        }

        return String.format("{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"id\": 2,\n"
            + "    \"identifier\": \"FK2/QZZSST\",\n"
            + "    \"persistentUrl\": \"https://doi.org/10.5072/FK2/QZZSST\",\n"
            + "    \"latestVersion\": {\n"
            + "      \"id\": 2,\n"
            + "      \"datasetId\": 2,\n"
            + "      \"datasetPersistentId\": \"%s\",\n"
            + "      \"storageIdentifier\": \"file://10.5072/FK2/QZZSST\",\n"
            + "      \"fileAccessRequest\": false,\n"
            + "      \"metadataBlocks\": {\n"
            + "        \"dansDataVaultMetadata\": {\n"
            + "          \"displayName\": \"Data Vault Metadata\",\n"
            + "          \"name\": \"dansDataVaultMetadata\",\n"
            + "          \"fields\": [\n"
            + "            {\n"
            + "              \"typeName\": \"dansSwordToken\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:uuid:2cd3745a-8b42-44a7-b1ca-5c93aa6f4e32\"\n"
            + "            },\n"
            + "            {\n"
            + "              \"typeName\": \"dansOtherId\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": %s\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}", persistentId, dansOtherId);
    }
}
