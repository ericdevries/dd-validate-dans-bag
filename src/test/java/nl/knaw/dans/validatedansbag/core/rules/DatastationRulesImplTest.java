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
import nl.knaw.dans.validatedansbag.core.engine.RuleViolationDetailsException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatastationRulesImplTest {

    final BagItMetadataReader bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
    final DataverseServiceImpl dataverseService = new DataverseServiceImpl(new DataverseConfig());
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
    void testOrganizationIdentifierIsValid() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn(List.of("org-identifier"))
            .when(bagItMetadataReader).getField(Mockito.any(), Mockito.anyString());

        Mockito.doReturn("urn:uuid:2cd3745a-8b42-44a7-b1ca-5c93aa6f4e32")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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
            + "        \"global_id\": \"doi:10.5072/FK2/QZZSST\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"count_in_response\": 1\n"
            + "  }\n"
            + "}";

        var latestVersionResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"id\": 2,\n"
            + "    \"identifier\": \"FK2/QZZSST\",\n"
            + "    \"persistentUrl\": \"https://doi.org/10.5072/FK2/QZZSST\",\n"
            + "    \"latestVersion\": {\n"
            + "      \"id\": 2,\n"
            + "      \"datasetId\": 2,\n"
            + "      \"datasetPersistentId\": \"doi:10.5072/FK2/QZZSST\",\n"
            + "      \"storageIdentifier\": \"file://10.5072/FK2/QZZSST\",\n"
            + "      \"fileAccessRequest\": false,\n"
            + "      \"metadataBlocks\": {\n"
            + "        \"dansDataVaultMetadata\": {\n"
            + "          \"displayName\": \"Data Vault Metadata\",\n"
            + "          \"name\": \"dansDataVaultMetadata\",\n"
            + "          \"fields\": [\n"
            + "            {\n"
            + "              \"typeName\": \"dansBagId\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:uuid:2cd3745a-8b42-44a7-b1ca-5c93aa6f4e32\"\n"
            + "            },\n"
            + "            {\n"
            + "              \"typeName\": \"dansNbn\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:nbn:nl:ui:13-025de6e2-bdcf-4622-b134-282b4c590f42\"\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(fakeSearchResults))
            .thenReturn(createStringResponse(latestVersionResult));

        assertDoesNotThrow(() -> checker.organizationalIdentifierIsValid().validate(Path.of("bagdir")));

    }

    @Test
    void testOrganizationIdentifierHasMoreThanOneEntry() {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn(List.of("org-identifier", "org-identifier2"))
            .when(bagItMetadataReader).getField(Mockito.any(), Mockito.anyString());

        Mockito.doReturn("urn:uuid:2cd3745a-8b42-44a7-b1ca-5c93aa6f4e32")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        assertThrows(RuleViolationDetailsException.class, () -> checker.organizationalIdentifierIsValid().validate(Path.of("bagdir")));
    }

    @Test
    void testOrganizationIdentifierButIsVersionOfIsNotSet() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn(List.of("org-identifier"))
            .when(bagItMetadataReader).getField(Mockito.any(), Mockito.anyString());

        Mockito.doReturn(null)
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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
            + "        \"global_id\": \"doi:10.5072/FK2/QZZSST\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"count_in_response\": 1\n"
            + "  }\n"
            + "}";

        var latestVersionResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"id\": 2,\n"
            + "    \"identifier\": \"FK2/QZZSST\",\n"
            + "    \"persistentUrl\": \"https://doi.org/10.5072/FK2/QZZSST\",\n"
            + "    \"latestVersion\": {\n"
            + "      \"id\": 2,\n"
            + "      \"datasetId\": 2,\n"
            + "      \"datasetPersistentId\": \"doi:10.5072/FK2/QZZSST\",\n"
            + "      \"storageIdentifier\": \"file://10.5072/FK2/QZZSST\",\n"
            + "      \"fileAccessRequest\": false,\n"
            + "      \"metadataBlocks\": {\n"
            + "        \"dansDataVaultMetadata\": {\n"
            + "          \"displayName\": \"Data Vault Metadata\",\n"
            + "          \"name\": \"dansDataVaultMetadata\",\n"
            + "          \"fields\": [\n"
            + "            {\n"
            + "              \"typeName\": \"dansBagId\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:uuid:2cd3745a-8b42-44a7-b1ca-5c93aa6f4e32\"\n"
            + "            },\n"
            + "            {\n"
            + "              \"typeName\": \"dansNbn\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:nbn:nl:ui:13-025de6e2-bdcf-4622-b134-282b4c590f42\"\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(fakeSearchResults))
            .thenReturn(createStringResponse(latestVersionResult));

        assertThrows(RuleViolationDetailsException.class,
            () -> checker.organizationalIdentifierIsValid().validate(Path.of("bagdir")));

    }

    @Test
    void testOrganizationIdentifierAndNoMatchingBags() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn(List.of("org-identifier"))
            .when(bagItMetadataReader).getField(Mockito.any(), Mockito.anyString());

        Mockito.doReturn("bag")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(fakeSearchResults));

        assertDoesNotThrow(() -> checker.organizationalIdentifierIsValid().validate(Path.of("bagdir")));
    }

    @Test
    void testOrganizationIdentifierIsValidButTheDepositIsDifferent() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn(List.of("org-identifier"))
            .when(bagItMetadataReader).getField(Mockito.any(), Mockito.anyString());

        Mockito.doReturn("urn:uuid:a-different-uuid")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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
            + "        \"global_id\": \"doi:10.5072/FK2/QZZSST\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"count_in_response\": 1\n"
            + "  }\n"
            + "}";

        var latestVersionResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"id\": 2,\n"
            + "    \"identifier\": \"FK2/QZZSST\",\n"
            + "    \"persistentUrl\": \"https://doi.org/10.5072/FK2/QZZSST\",\n"
            + "    \"latestVersion\": {\n"
            + "      \"id\": 2,\n"
            + "      \"datasetId\": 2,\n"
            + "      \"datasetPersistentId\": \"doi:10.5072/FK2/QZZSST\",\n"
            + "      \"storageIdentifier\": \"file://10.5072/FK2/QZZSST\",\n"
            + "      \"fileAccessRequest\": false,\n"
            + "      \"metadataBlocks\": {\n"
            + "        \"dansDataVaultMetadata\": {\n"
            + "          \"displayName\": \"Data Vault Metadata\",\n"
            + "          \"name\": \"dansDataVaultMetadata\",\n"
            + "          \"fields\": [\n"
            + "            {\n"
            + "              \"typeName\": \"dansBagId\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:uuid:2cd3745a-8b42-44a7-b1ca-5c93aa6f4e32\"\n"
            + "            },\n"
            + "            {\n"
            + "              \"typeName\": \"dansNbn\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:nbn:nl:ui:13-025de6e2-bdcf-4622-b134-282b4c590f42\"\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(fakeSearchResults))
            .thenReturn(createStringResponse(latestVersionResult));

        assertThrows(RuleViolationDetailsException.class,
            () -> checker.organizationalIdentifierIsValid().validate(Path.of("bagdir")));

    }

    @Test
    void testOrganizationIdentifierIsValidButTheBagIdIsMissing() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito.doReturn(List.of("org-identifier"))
            .when(bagItMetadataReader).getField(Mockito.any(), Mockito.anyString());

        Mockito.doReturn("urn:uuid:a-different-uuid")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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
            + "        \"global_id\": \"doi:10.5072/FK2/QZZSST\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"count_in_response\": 1\n"
            + "  }\n"
            + "}";

        var latestVersionResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"id\": 2,\n"
            + "    \"identifier\": \"FK2/QZZSST\",\n"
            + "    \"persistentUrl\": \"https://doi.org/10.5072/FK2/QZZSST\",\n"
            + "    \"latestVersion\": {\n"
            + "      \"id\": 2,\n"
            + "      \"datasetId\": 2,\n"
            + "      \"datasetPersistentId\": \"doi:10.5072/FK2/QZZSST\",\n"
            + "      \"storageIdentifier\": \"file://10.5072/FK2/QZZSST\",\n"
            + "      \"fileAccessRequest\": false,\n"
            + "      \"metadataBlocks\": {\n"
            + "        \"dansDataVaultMetadata\": {\n"
            + "          \"displayName\": \"Data Vault Metadata\",\n"
            + "          \"name\": \"dansDataVaultMetadata\",\n"
            + "          \"fields\": [\n"
            + "            {\n"
            + "              \"typeName\": \"dansNbn\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:nbn:nl:ui:13-025de6e2-bdcf-4622-b134-282b4c590f42\"\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

        Mockito.when(httpClient.execute(Mockito.any()))
            .thenReturn(createStringResponse(fakeSearchResults))
            .thenReturn(createStringResponse(latestVersionResult));

        assertThrows(RuleViolationDetailsException.class,
            () -> checker.organizationalIdentifierIsValid().validate(Path.of("bagdir")));

    }

    @Test
    void dataStationUserAccountIsAuthorized() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn("user-account-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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
            + "        \"global_id\": \"doi:10.5072/FK2/QZZSST\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"count_in_response\": 1\n"
            + "  }\n"
            + "}";

        var assignmentResult = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@user-account-name\",\n"
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
            .thenReturn(createStringResponse(fakeSearchResults))
            .thenReturn(createStringResponse(assignmentResult));

        assertDoesNotThrow(() -> checker.dataStationUserAccountIsAuthorized().validate(Path.of("bagdir")));

    }

    @Test
    void dataStationUserAccountIsNotAuthorized() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn("user-account-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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
            + "        \"global_id\": \"doi:10.5072/FK2/QZZSST\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"count_in_response\": 1\n"
            + "  }\n"
            + "}";

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
            .thenReturn(createStringResponse(fakeSearchResults))
            .thenReturn(createStringResponse(assignmentResult));

        var e = assertThrows(RuleViolationDetailsException.class,
            () -> checker.dataStationUserAccountIsAuthorized().validate(Path.of("bagdir")));

        assertTrue(e.getMessage().contains("Data Station account that is authorized to deposit the bag"));

    }

    @Test
    void dataStationUserAccountIsNotSet() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn(null)
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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
            + "        \"global_id\": \"doi:10.5072/FK2/QZZSST\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"count_in_response\": 1\n"
            + "  }\n"
            + "}";

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
            .thenReturn(createStringResponse(fakeSearchResults))
            .thenReturn(createStringResponse(assignmentResult));

        assertDoesNotThrow(() -> checker.dataStationUserAccountIsAuthorized().validate(Path.of("bagdir")));

    }

    @Test
    void dataStationUserAccountYieldsNoSearchResults() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn("user-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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
            .thenReturn(createStringResponse(fakeSearchResults))
            .thenReturn(createStringResponse(assignmentResult));

        var e = assertThrows(RuleViolationDetailsException.class,
            () -> checker.dataStationUserAccountIsAuthorized().validate(Path.of("bagdir")));

        assertTrue(e.getMessage().contains("it must be a valid SWORD token in the data station"));
    }

    @Test
    void dataStationUserAccountIsNotAuthorizedButHasADifferentRole() throws Exception {
        var dv = createDataverseServiceSpy();
        var checker = new DatastationRulesImpl(bagItMetadataReader, dv);

        Mockito
            .doReturn("user-account-name")
            .doReturn("urn:uuid:the_id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        // trimmed down versions of what dataverse would actually spit out
        var fakeSearchResults = "{\n"
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
            + "        \"global_id\": \"doi:10.5072/FK2/QZZSST\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"count_in_response\": 1\n"
            + "  }\n"
            + "}";

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
            .thenReturn(createStringResponse(fakeSearchResults))
            .thenReturn(createStringResponse(assignmentResult));

        var e = assertThrows(RuleViolationDetailsException.class,
            () -> checker.dataStationUserAccountIsAuthorized().validate(Path.of("bagdir")));

        assertTrue(e.getMessage().contains("Data Station account that is authorized to deposit the bag"));

    }
}
