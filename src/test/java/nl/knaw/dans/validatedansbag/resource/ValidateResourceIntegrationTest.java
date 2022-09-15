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
package nl.knaw.dans.validatedansbag.resource;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetLatestVersion;
import nl.knaw.dans.lib.dataverse.model.search.SearchResult;
import nl.knaw.dans.openapi.api.ValidateCommandDto;
import nl.knaw.dans.openapi.api.ValidateOkDto;
import nl.knaw.dans.openapi.api.ValidateOkRuleViolationsDto;
import nl.knaw.dans.validatedansbag.core.engine.RuleEngineImpl;
import nl.knaw.dans.validatedansbag.core.rules.BagRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.DatastationRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.FilesXmlRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.TestLicenseConfig;
import nl.knaw.dans.validatedansbag.core.rules.XmlRulesImpl;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.FileServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.RuleEngineServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlSchemaValidator;
import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.PolygonListValidatorImpl;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
class ValidateResourceIntegrationTest {
    public static final ResourceExtension EXT;

    private static final DataverseService dataverseService = Mockito.mock(DataverseService.class);
    private static final XmlSchemaValidator xmlSchemaValidator = Mockito.mock(XmlSchemaValidator.class);

    static {
        EXT = ResourceExtension.builder()
            .addProvider(MultiPartFeature.class)
            .addProvider(ValidateOkDtoYamlMessageBodyWriter.class)
            .addResource(buildValidateResource())
            .build();
    }

    static ValidateResource buildValidateResource() {

        var fileService = new FileServiceImpl();
        var bagItMetadataReader = new BagItMetadataReaderImpl();
        var xmlReader = new XmlReaderImpl();
        var daiDigestCalculator = new IdentifierValidatorImpl();
        var polygonListValidator = new PolygonListValidatorImpl();
        var originalFilepathsService = new OriginalFilepathsServiceImpl(fileService);
        var licenseValidator = new LicenseValidatorImpl(new TestLicenseConfig());

        // set up the different rule implementations
        var bagRules = new BagRulesImpl(fileService, bagItMetadataReader, xmlReader, originalFilepathsService, daiDigestCalculator, polygonListValidator, licenseValidator);
        var filesXmlRules = new FilesXmlRulesImpl(xmlReader, fileService, originalFilepathsService);
        var xmlRules = new XmlRulesImpl(xmlReader, xmlSchemaValidator, fileService);
        var datastationRules = new DatastationRulesImpl(bagItMetadataReader, dataverseService);

        // set up the engine and the service that has a default set of rules
        var ruleEngine = new RuleEngineImpl();
        var ruleEngineService = new RuleEngineServiceImpl(ruleEngine, bagRules, xmlRules, filesXmlRules, fileService, datastationRules);

        return new ValidateResource(ruleEngineService, fileService);
    }

    @BeforeEach
    void setup() {
        Mockito.reset(dataverseService);
        Mockito.reset(xmlSchemaValidator);
    }

    @Test
    void validateFormDataWithInvalidBag() {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("bags/audiences-invalid")).getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(filename);
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);
        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        assertFalse(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(ValidateOkDto.InformationPackageTypeEnum.DEPOSIT, response.getInformationPackageType());
        assertEquals(filename, response.getBagLocation());
        assertEquals(1, response.getRuleViolations().size());
    }

    @Test
    void validateFormDataWithSomeException() throws Exception {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("bags/valid-bag")).getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(filename);
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);
        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        Mockito.when(xmlSchemaValidator.validateDocument(Mockito.any(), Mockito.anyString()))
            .thenThrow(new SAXException("Something is broken"));

        try (var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), Response.class)) {

            assertEquals(500, response.getStatus());
        }
    }

    @Test
    void validateFormDataWithValidBagAndOriginalFilepaths() {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("bags/original-filepaths-valid-bag")).getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(filename);
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.MIGRATION);
        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        assertTrue(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(ValidateOkDto.InformationPackageTypeEnum.MIGRATION, response.getInformationPackageType());
        assertEquals(filename, response.getBagLocation());
        assertEquals(0, response.getRuleViolations().size());
    }

    @Test
    void validateFormDataWithInValidBagAndOriginalFilepaths() {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("bags/original-filepaths-invalid-bag")).getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(filename);
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.MIGRATION);
        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        assertFalse(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(ValidateOkDto.InformationPackageTypeEnum.MIGRATION, response.getInformationPackageType());
        assertEquals(filename, response.getBagLocation());
        assertEquals(3, response.getRuleViolations().size());
    }

    @Test
    void validateMultipartZipFile() throws Exception {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("zips/audiences.zip"));

        var data = new ValidateCommandDto();
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);
        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE)
            .field("zip", filename.openStream(), MediaType.valueOf("application/zip"));

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        assertTrue(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(ValidateOkDto.InformationPackageTypeEnum.DEPOSIT, response.getInformationPackageType());
        assertNull(response.getBagLocation());
        assertEquals(0, response.getRuleViolations().size());
    }

    @Test
    void validateZipFile() throws Exception {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("zips/audiences.zip"));

        var data = new ValidateCommandDto();
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);
        var response = EXT.target("/validate")
            .request()
            .post(Entity.entity(filename.openStream(), MediaType.valueOf("application/zip")), ValidateOkDto.class);

        assertTrue(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(ValidateOkDto.InformationPackageTypeEnum.DEPOSIT, response.getInformationPackageType());
        assertNull(response.getBagLocation());
        assertEquals(0, response.getRuleViolations().size());
    }

    @Test
    void validateZipFileAndGetTextResponse() throws Exception {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("zips/invalid-sha1.zip"));

        var data = new ValidateCommandDto();
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);

        var response = EXT.target("/validate")
            .request()
            .header("accept", "text/plain")
            .post(Entity.entity(filename.openStream(), MediaType.valueOf("application/zip")), String.class);

        assertTrue(response.contains("Bag location:"));
        assertTrue(response.contains("Name:"));
        assertTrue(response.contains("Profile version:"));
        assertTrue(response.contains("Information package type:"));
        assertTrue(response.contains("Is compliant:"));
        assertTrue(response.contains("Rule violations:"));
    }

    @Test
    void validateMultipartDataLocationCouldNotBeRead() {
        var data = new ValidateCommandDto();
        data.setBagLocation("/some/non/existing/filename");
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        try (var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), Response.class)) {

            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void validateWithHasOrganizationalIdentifier() throws Exception {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("bags/bag-with-is-version-of")).getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(filename);
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var searchResultsJson = "{\n"
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

        var latestVersionJson = "{\n"
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
            + "              \"typeName\": \"dansSwordToken\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:uuid:34632f71-11f8-48d8-9bf3-79551ad22b5e\"\n"
            + "            },\n"
            + "            {\n"
            + "              \"typeName\": \"dansOtherId\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"organizational-identifier\"\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

        var dataverseRoleAssignmentsJson = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@eric\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"datasetcreator\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    }\n"
            + "  ]\n"
            + "}";

        var datasetRoleAssignmentsJson = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@eric\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"dataseteditor\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        var searchResult = new MockedDataverseResponse<SearchResult>(searchResultsJson, SearchResult.class);
        var latestVersionResult = new MockedDataverseResponse<DatasetLatestVersion>(latestVersionJson, DatasetLatestVersion.class);
        var swordTokenResult = new MockedDataverseResponse<SearchResult>(searchResultsJson, SearchResult.class);
        var dataverseRoleAssignmentsResult = new MockedDataverseResponse<List<RoleAssignmentReadOnly>>(dataverseRoleAssignmentsJson, List.class, RoleAssignmentReadOnly.class);
        var datasetRoleAssignmentsResult = new MockedDataverseResponse<List<RoleAssignmentReadOnly>>(datasetRoleAssignmentsJson, List.class, RoleAssignmentReadOnly.class);

        Mockito.when(dataverseService.getDataverseRoleAssignments(Mockito.anyString()))
            .thenReturn(dataverseRoleAssignmentsResult);

        Mockito.when(dataverseService.getDatasetRoleAssignments(Mockito.anyString()))
            .thenReturn(datasetRoleAssignmentsResult);

        Mockito.when(dataverseService.getAllowedCreatorRole()).thenReturn("datasetcreator");
        Mockito.when(dataverseService.getAllowedEditorRole()).thenReturn("dataseteditor");
        Mockito.when(dataverseService.searchDatasetsByOrganizationalIdentifier(Mockito.anyString()))
            .thenReturn(searchResult);

        Mockito.when(dataverseService.getDataset(Mockito.anyString()))
            .thenReturn(latestVersionResult);

        Mockito.when(dataverseService.searchBySwordToken(Mockito.anyString()))
            .thenReturn(swordTokenResult);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        assertTrue(response.getIsCompliant());
        assertEquals("bag-with-is-version-of", response.getName());
    }

    @Test
    void validateWithHasOrganizationalIdentifierButItDoesNotMatch() throws Exception {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("bags/bag-with-is-version-of")).getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(filename);
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var searchResultsJson = "{\n"
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

        var latestVersionJson = "{\n"
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
            + "              \"typeName\": \"dansOtherId\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:uuid:wrong-uuid\"\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

        var dataverseRoleAssignmentsJson = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@eric\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"datasetcreator\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    }\n"
            + "  ]\n"
            + "}";

        var datasetRoleAssignmentsJson = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@eric\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"dataseteditor\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    }\n"
            + "  ]\n"
            + "}";

        var searchResult = new MockedDataverseResponse<SearchResult>(searchResultsJson, SearchResult.class);
        var latestVersionResult = new MockedDataverseResponse<DatasetLatestVersion>(latestVersionJson, DatasetLatestVersion.class);
        var swordTokenResult = new MockedDataverseResponse<SearchResult>(searchResultsJson, SearchResult.class);
        var dataverseRoleAssignmentsResult = new MockedDataverseResponse<List<RoleAssignmentReadOnly>>(dataverseRoleAssignmentsJson, List.class, RoleAssignmentReadOnly.class);
        var datasetRoleAssignmentsResult = new MockedDataverseResponse<List<RoleAssignmentReadOnly>>(datasetRoleAssignmentsJson, List.class, RoleAssignmentReadOnly.class);

        Mockito.when(dataverseService.getAllowedCreatorRole()).thenReturn("datasetcreator");
        Mockito.when(dataverseService.getAllowedEditorRole()).thenReturn("dataseteditor");

        Mockito.when(dataverseService.searchDatasetsByOrganizationalIdentifier(Mockito.anyString()))
            .thenReturn(searchResult);

        Mockito.when(dataverseService.getDataset(Mockito.anyString()))
            .thenReturn(latestVersionResult);

        Mockito.when(dataverseService.searchBySwordToken(Mockito.anyString()))
            .thenReturn(swordTokenResult);

        Mockito.when(dataverseService.getDataverseRoleAssignments(Mockito.anyString()))
            .thenReturn(dataverseRoleAssignmentsResult);

        Mockito.when(dataverseService.getDatasetRoleAssignments(Mockito.anyString()))
            .thenReturn(datasetRoleAssignmentsResult);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        var failed = response.getRuleViolations().stream()
            .map(ValidateOkRuleViolationsDto::getRule).collect(Collectors.toSet());

        assertEquals(Set.of("4.3(b)"), failed);
        assertFalse(response.getIsCompliant());
        assertEquals("bag-with-is-version-of", response.getName());
    }

    @Test
    void validateWithNoMatchingSwordToken() throws Exception {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("bags/bag-with-is-version-of")).getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(filename);
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var searchResultsJson = "{\n"
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

        var swordTokenJson = "{\n"
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

        var latestVersionJson = "{\n"
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
            + "              \"typeName\": \"dansSwordToken\",\n"
            + "              \"multiple\": false,\n"
            + "              \"typeClass\": \"primitive\",\n"
            + "              \"value\": \"urn:uuid:34632f71-11f8-48d8-9bf3-79551ad22b5e\"\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";
        var searchResult = new MockedDataverseResponse<SearchResult>(searchResultsJson, SearchResult.class);
        var latestVersionResult = new MockedDataverseResponse<DatasetLatestVersion>(latestVersionJson, DatasetLatestVersion.class);
        var swordTokenResult = new MockedDataverseResponse<SearchResult>(swordTokenJson, SearchResult.class);

        Mockito.when(dataverseService.searchDatasetsByOrganizationalIdentifier(Mockito.anyString()))
            .thenReturn(searchResult);

        Mockito.when(dataverseService.getDataset(Mockito.anyString()))
            .thenReturn(latestVersionResult);

        Mockito.when(dataverseService.searchBySwordToken(Mockito.anyString()))
            .thenReturn(swordTokenResult);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        var failed = response.getRuleViolations().stream()
            .map(ValidateOkRuleViolationsDto::getRule).collect(Collectors.toSet());

        assertEquals(Set.of("4.2", "4.3(a)", "4.3(b)", "4.3(c)"), failed);
        assertFalse(response.getIsCompliant());
        assertEquals("bag-with-is-version-of", response.getName());
    }
}
