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

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseClientConfig;
import nl.knaw.dans.validatedansbag.core.config.DataverseConfig;
import nl.knaw.dans.validatedansbag.core.config.SwordDepositorRoles;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.DataverseServiceImpl;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatastationRulesImplTest {

    final BagItMetadataReader bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
    final DataverseServiceImpl dataverseService = new DataverseServiceImpl(new DataverseConfig("", "", new SwordDepositorRoles("datasetcreator", "dataseteditor")));
    final HttpClient httpClient = Mockito.mock(HttpClient.class);

    @AfterEach
    void afterEach() {
        Mockito.reset(bagItMetadataReader);
        Mockito.reset(httpClient);
    }

    HttpResponse createStringResponse(String str) throws UnsupportedEncodingException {
        var statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");
        var response = new BasicHttpResponse(statusLine);
        response.setEntity(new StringEntity(str));

        return response;
    }

    DataverseService createDataverseServiceSpy() {
        var dv = Mockito.spy(dataverseService);
        var config = new DataverseClientConfig(URI.create("http://localhost:8080"));
        var client = new DataverseClient(config, httpClient, new ObjectMapper());

        Mockito.doReturn(client).when(dv).getDataverseClient();

        return dv;
    }

    @Test
    void bagExistsInDatastation() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn("is-version-of-id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var doi = "doi:10.5072/FK2/QZZSST";
        var searchResult = getSearchResult(doi);
        var latestVersionResult = getLatestVersion(doi, null);

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(latestVersionResult));

        var result = checker.bagExistsInDatastation().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void bagNotExistsInDatastation() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn("is-version-of-id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var searchResult = getEmptySearchResult();

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult));

        var result = checker.bagExistsInDatastation().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDataset() throws Exception {

        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn("dans-other-id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var doi = "doi:10.5072/FK2/QZZSST";
        var dansOtherId = "dans-other-id";
        var searchResult = getSearchResult(doi);
        var latestVersionResult = getLatestVersion(doi, dansOtherId);

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(latestVersionResult));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDatasetBothAreNull() throws Exception {

        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn(null)
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var doi = "doi:10.5072/FK2/QZZSST";
        var searchResult = getSearchResult(doi);
        var latestVersionResult = getLatestVersion(doi, null);

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(latestVersionResult));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDatasetActualIsNull() throws Exception {

        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.anyString()))
            .thenReturn("is_version_of")
            .thenReturn("has_organizational_identifier");

        var doi = "doi:10.5072/FK2/QZZSST";
        var searchResult = getSearchResult(doi);
        var latestVersionResult = getLatestVersion(doi, null);

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(latestVersionResult));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDatasetMismatch() throws Exception {

        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.anyString()))
            .thenReturn("is_version_of")
            .thenReturn("has_organizational_identifier");

        var doi = "doi:10.5072/FK2/QZZSST";
        var searchResult = getSearchResult(doi);
        var latestVersionResult = getLatestVersion(doi, "some_other_organizational_identifier");

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(latestVersionResult));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    // CREATE tests
    @Test
    void dataStationUserAccountIsAuthorizedToCreate() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(assignmentResult));

        var result = checker.userIsAuthorizedToCreateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void dataStationUserAccountIsNotAuthorizedToCreate() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(assignmentResult));

        var result = checker.userIsAuthorizedToCreateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void dataStationUserAccountIsNotSetCreate() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(assignmentResult));

        var result = checker.userIsAuthorizedToCreateDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SKIP_DEPENDENCIES, result.getStatus());
    }

    @Test
    void dataStationUserAccountYieldsNoSearchResultsCreate() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(assignmentResult));

        var result = checker.userIsAuthorizedToCreateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    // EDIT tests

    @Test
    void dataStationUserAccountIsAuthorizedToEdit() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn("user-account-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var searchResult = getSearchResult("doi");
        var latestVersionResult = getLatestVersion("doi", "otherid");

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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(latestVersionResult))
            .thenReturn(createStringResponse(assignmentResult));

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());

        Mockito.verify(dv).getDataset(Mockito.eq("doi"));
    }

    @Test
    void dataStationUserAccountIsNotAuthorizedToEdit() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn("user-account-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var searchResult = getSearchResult("doi");
        var latestVersionResult = getLatestVersion("doi", "otherid");
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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(latestVersionResult))
            .thenReturn(createStringResponse(assignmentResult));

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        Mockito.verify(dv).getDataset(Mockito.eq("doi"));
    }

    @Test
    void dataStationUserAccountIsNotAuthorizedToEditButHasADifferentRole() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn("user-account-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var searchResult = getSearchResult("doi");
        var latestVersionResult = getLatestVersion("doi", "otherid");
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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(latestVersionResult))
            .thenReturn(createStringResponse(assignmentResult));

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertTrue(result.getErrorMessages().get(0).contains("user-account-name"));
        assertTrue(result.getErrorMessages().get(0).contains("expected: dataseteditor"));
        assertTrue(result.getErrorMessages().get(0).contains("readonly"));

    }

    @Test
    void dataStationUserAccountIsNotSetEdit() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn(null)
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var searchResult = getSearchResult("doi");
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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(assignmentResult));

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SKIP_DEPENDENCIES, result.getStatus());

    }

    @Test
    void dataStationUserAccountYieldsNoSearchResultsEdit() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn("user-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var searchResult = getEmptySearchResult();

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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(searchResult))
            .thenReturn(createStringResponse(assignmentResult));

        var result = checker.userIsAuthorizedToUpdateDataset().validate(Path.of("bagdir"));

        assertTrue(result.getErrorMessages().get(0).contains("it must be a valid SWORD token in the data station"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
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
