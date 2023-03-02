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
import nl.knaw.dans.lib.dataverse.model.DataMessage;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetLatestVersion;
import nl.knaw.dans.lib.dataverse.model.search.SearchResult;
import nl.knaw.dans.validatedansbag.core.config.SwordDepositorRoles;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import nl.knaw.dans.validatedansbag.resources.util.MockedDataverseResponse;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatastationRulesImplTest {

    final BagItMetadataReader bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
    final DataverseService dataverseService = Mockito.mock(DataverseService.class);
    final SwordDepositorRoles swordDepositorRoles = new SwordDepositorRoles("datasetcreator", "dataseteditor");
    final XmlReader xmlReader = Mockito.mock(XmlReader.class);

    DatastationRulesImplTest() {
    }

    private Document parseXmlString(String str) throws ParserConfigurationException, IOException, SAXException {
        return new XmlReaderImpl().readXmlString(str);
    }

    @AfterEach
    void afterEach() {
        Mockito.reset(bagItMetadataReader);
        Mockito.reset(dataverseService);
        Mockito.reset(xmlReader);
    }

    @Test
    void bagExistsInDatastation_should_return_SUCCESS_if_bag_exists() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles, xmlReader);

        Mockito.doReturn("urn:uuid:is-version-of-id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, null));

        var result = checker.bagExistsInDatastation().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void bagExistsInDatastation_should_return_ERROR_when_search_yields_zero_results() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles, xmlReader);

        Mockito.doReturn("urn:uuid:is-version-of-id")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        String emptySearchResult = getEmptySearchResult();
        mockSearchBySwordToken(emptySearchResult);

        var result = checker.bagExistsInDatastation().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDataset_should_return_SUCCESS_if_otherId_matches_hasOrganizationalIdentifier() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles, xmlReader);

        var isVersionOf = "urn:uuid:some-uuid";
        var otherId = "other-id";
        var hasOrganizationalIdentifier = "other-id";

        Mockito.doReturn(isVersionOf)
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.eq("Is-Version-Of"));

        Mockito.doReturn(hasOrganizationalIdentifier)
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.eq("Has-Organizational-Identifier"));

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, otherId));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDataset_should_return_SUCCESS_if_both_values_are_null() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles, xmlReader);

        Mockito.doReturn("urn:uuid:some-uuid")
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.eq("Is-Version-Of"));

        Mockito.doReturn(null)
            .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.eq("Has-Organizational-Identifier"));

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, null));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDataset_should_return_ERROR_when_dataset_is_null_and_metadata_is_not_null() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles, xmlReader);

        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.anyString()))
            .thenReturn("urn:uuid:is_version_of")
            .thenReturn("has_organizational_identifier");

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, null));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void organizationalIdentifierExistsInDataset_should_return_ERROR_if_values_do_not_match() throws Exception {
        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles, xmlReader);

        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.anyString()))
            .thenReturn("urn:uuid:is_version_of")
            .thenReturn("has_organizational_identifier");

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, "some_other_organizational_identifier"));

        var result = checker.organizationalIdentifierExistsInDataset().validate(Path.of("bagdir"));
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

    String getMaxEmbargoInMonths(int months) {
        return "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"message\": \"" + months + "\"\n"
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

    @Test
    void embargoPeriodIsTooLong() throws Exception {
        int embargoPeriodInMonths = 4;
        DateTime dateTime = new DateTime(DateTime.now().plusMonths(embargoPeriodInMonths).plusDays(1));
        final String xml = "<ddm:DDM\n"
            + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
            + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
            + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
            + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
            + "    <ddm:profile>\n"
            + "        <ddm:available>" + DateTimeFormat.forPattern("yyyy-MM-dd").print(dateTime) + "</ddm:available>\n"
            + "    </ddm:profile>\n"
            + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles, reader);

        var embargoResultJson = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"message\": \"" + embargoPeriodInMonths + "\"\n"
            + "  }\n"
            + "}";
        var maxEmbargoDurationResult = new MockedDataverseResponse<DataMessage>(embargoResultJson, DataMessage.class);
        Mockito.when(dataverseService.getMaxEmbargoDurationInMonths())
            .thenReturn(maxEmbargoDurationResult);

        var result = checker.embargoPeriodWithinLimits().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void embargoPeriodIsNotTooLong() throws Exception {
        int embargoPeriodInMonths = 4;
        DateTime dateTime = new DateTime(DateTime.now().plusMonths(embargoPeriodInMonths));
        final String xml = "<ddm:DDM\n"
            + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
            + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
            + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
            + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
            + "    <ddm:profile>\n"
            + "        <ddm:available>" + DateTimeFormat.forPattern("yyyy-MM-dd").print(dateTime) + "</ddm:available>\n"
            + "    </ddm:profile>\n"
            + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles, reader);

        var embargoResultJson = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"message\": \"" + embargoPeriodInMonths + "\"\n"
            + "  }\n"
            + "}";
        var maxEmbargoDurationResult = new MockedDataverseResponse<DataMessage>(embargoResultJson, DataMessage.class);
        Mockito.when(dataverseService.getMaxEmbargoDurationInMonths())
            .thenReturn(maxEmbargoDurationResult);

        var result = checker.embargoPeriodWithinLimits().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void embargoDateWithoutDayPartIsAccepted() throws Exception {
        int embargoPeriodInMonths = 4;
        final String xml = "<ddm:DDM\n"
            + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
            + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
            + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
            + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
            + "    <ddm:profile>\n"
            + "        <ddm:available>2013-12</ddm:available>\n"
            + "    </ddm:profile>\n"
            + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var checker = new DatastationRulesImpl(bagItMetadataReader, dataverseService, swordDepositorRoles, reader);
        var embargoResultJson = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": {\n"
            + "    \"message\": \"" + embargoPeriodInMonths + "\"\n"
            + "  }\n"
            + "}";
        var maxEmbargoDurationResult = new MockedDataverseResponse<DataMessage>(embargoResultJson, DataMessage.class);
        Mockito.when(dataverseService.getMaxEmbargoDurationInMonths())
            .thenReturn(maxEmbargoDurationResult);

        var result = checker.embargoPeriodWithinLimits().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }
}
