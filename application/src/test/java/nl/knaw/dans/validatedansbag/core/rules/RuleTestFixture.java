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
import nl.knaw.dans.lib.dataverse.model.search.SearchResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.FilesXmlService;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsService;
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlSchemaValidator;
import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidator;
import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidator;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.PolygonListValidator;
import nl.knaw.dans.validatedansbag.core.validator.PolygonListValidatorImpl;
import nl.knaw.dans.validatedansbag.resources.util.MockedDataverseResponse;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class RuleTestFixture {
    protected final FileService fileService = Mockito.mock(FileService.class);
    protected final XmlReader xmlReader = Mockito.mock(XmlReader.class);
    protected final IdentifierValidator identifierValidator = new IdentifierValidatorImpl();
    protected final BagItMetadataReader bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
    protected final PolygonListValidator polygonListValidator = new PolygonListValidatorImpl();
    protected final OriginalFilepathsService originalFilepathsService = Mockito.mock(OriginalFilepathsService.class);
    protected final DataverseService dataverseService = Mockito.mock(DataverseService.class);

    protected final LicenseValidator licenseValidator = new LicenseValidatorImpl(dataverseService);
    protected final FilesXmlService filesXmlService = Mockito.mock(FilesXmlService.class);
    protected final XmlSchemaValidator xmlSchemaValidator = Mockito.mock(XmlSchemaValidator.class);

    @AfterEach
    void afterEach() {
        Mockito.reset(fileService);
        Mockito.reset(xmlReader);
        Mockito.reset(bagItMetadataReader);
        Mockito.reset(dataverseService);
        Mockito.reset(originalFilepathsService);
        Mockito.reset(filesXmlService);
    }

    protected Document parseXmlString(String str) throws ParserConfigurationException, IOException, SAXException {
        return new XmlReaderImpl().readXmlString(str);
    }

    protected void mockGetDataset(String json) throws IOException, DataverseException {
        var response = new MockedDataverseResponse<>(json, DatasetLatestVersion.class);
        Mockito.doReturn(response).when(dataverseService).getDataset(Mockito.anyString());
    }

    protected void mockSearchBySwordToken(String json) throws IOException, DataverseException {
        var response = new MockedDataverseResponse<>(json, SearchResult.class);
        Mockito.doReturn(response).when(dataverseService).searchBySwordToken(Mockito.any());
    }

    protected String getSearchResult(String globalId) {
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

    protected String getEmptySearchResult() {
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

    protected String getLatestVersion(String persistentId, String dansOtherId) {
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
