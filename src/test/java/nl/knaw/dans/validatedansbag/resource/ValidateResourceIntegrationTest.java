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
import nl.knaw.dans.openapi.api.ValidateCommandDto.LevelEnum;
import nl.knaw.dans.openapi.api.ValidateCommandDto.PackageTypeEnum;
import nl.knaw.dans.openapi.api.ValidateOkDto;
import nl.knaw.dans.openapi.api.ValidateOkDto.InformationPackageTypeEnum;
import nl.knaw.dans.openapi.api.ValidateOkRuleViolationsDto;
import nl.knaw.dans.validatedansbag.core.config.OtherIdPrefix;
import nl.knaw.dans.validatedansbag.core.config.SwordDepositorRoles;
import nl.knaw.dans.validatedansbag.core.engine.RuleEngineImpl;
import nl.knaw.dans.validatedansbag.core.rules.BagRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.DatastationRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.FilesXmlRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.TestLicenseConfig;
import nl.knaw.dans.validatedansbag.core.rules.XmlRulesImpl;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.FileServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.FilesXmlServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.RuleEngineServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlSchemaValidator;
import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.OrganizationIdentifierPrefixValidatorImpl;
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
import java.net.URL;
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
        var filesXmlService = new FilesXmlServiceImpl(xmlReader);

        var organizationIdentifierPrefixValidator = new OrganizationIdentifierPrefixValidatorImpl(
            List.of(new OtherIdPrefix("user001", "u1:"), new OtherIdPrefix("user002", "u2:"))
        );

        // set up the different rule implementations
        var bagRules = new BagRulesImpl(fileService, bagItMetadataReader, xmlReader, originalFilepathsService, daiDigestCalculator, polygonListValidator, licenseValidator,
            organizationIdentifierPrefixValidator, filesXmlService);
        var filesXmlRules = new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService);
        var xmlRules = new XmlRulesImpl(xmlReader, xmlSchemaValidator, fileService);
        var datastationRules = new DatastationRulesImpl(bagItMetadataReader, dataverseService, new SwordDepositorRoles("datasetcreator", "dataseteditor"));

        // set up the engine and the service that has a default set of rules
        var ruleEngine = new RuleEngineImpl();
        var ruleEngineService = new RuleEngineServiceImpl(ruleEngine, bagRules, xmlRules, filesXmlRules, fileService, datastationRules);

        return new ValidateResource(ruleEngineService, fileService);
    }

    static private Set<String> getViolatedRuleNumbers(ValidateOkDto response) {
        return response.getRuleViolations().stream()
            .map(ValidateOkRuleViolationsDto::getRule)
            .collect(Collectors.toSet());
    }

    private URL getResourceUrl(String name) {
        return Objects.requireNonNull(getClass().getClassLoader().getResource(name));
    }

    @BeforeEach
    void setup() {
        Mockito.reset(dataverseService);
        Mockito.reset(xmlSchemaValidator);
    }

    @Test
    void validateFormDataWithInvalidBag() {
        var bagDir = getResourceUrl("bags/invalid").getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(bagDir);
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.STAND_ALONE);
        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        assertFalse(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(InformationPackageTypeEnum.DEPOSIT, response.getInformationPackageType());
        assertEquals(bagDir, response.getBagLocation());
        assertTrue(response.getRuleViolations().size() != 0, "expecting rule violations, got none");
    }

    @Test
    void validateFormDataWithSomeException() throws Exception {
        var bagDir = getResourceUrl("bags/valid-bag").getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(bagDir);
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);
        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        Mockito.when(xmlSchemaValidator.validateDocument(Mockito.any(), Mockito.anyString()))
            .thenThrow(new SAXException("Something is broken"));

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), Response.class);

        assertEquals(500, response.getStatus());
    }

    @Test
    void validateFormDataWithValidBagAndOriginalFilepaths() throws Exception {
        var bagDir = getResourceUrl("bags/datastation-valid-bag").getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(bagDir);
        data.setPackageType(PackageTypeEnum.MIGRATION);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);

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


        var dataverseRoleAssignmentsJson = "{\n"
            + "  \"status\": \"OK\",\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 6,\n"
            + "      \"assignee\": \"@user001\",\n"
            + "      \"roleId\": 11,\n"
            + "      \"_roleAlias\": \"datasetcreator\",\n"
            + "      \"definitionPointId\": 2\n"
            + "    }\n"
            + "  ]\n"
            + "}";

        var swordTokenResult = new MockedDataverseResponse<SearchResult>(searchResultsJson, SearchResult.class);
        var dataverseRoleAssignmentsResult = new MockedDataverseResponse<List<RoleAssignmentReadOnly>>(dataverseRoleAssignmentsJson, List.class, RoleAssignmentReadOnly.class);

        Mockito.when(dataverseService.searchBySwordToken(Mockito.anyString()))
            .thenReturn(swordTokenResult);

        Mockito.when(dataverseService.getDataverseRoleAssignments(Mockito.anyString()))
            .thenReturn(dataverseRoleAssignmentsResult);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        assertTrue(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(InformationPackageTypeEnum.MIGRATION, response.getInformationPackageType());
        assertEquals(bagDir, response.getBagLocation());
        assertEquals(0, response.getRuleViolations().size());
    }

    @Test
    void validateFormDataWithInValidBagAndOriginalFilepaths() {
        var bagDir = getResourceUrl("bags/original-filepaths-invalid-bag").getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(bagDir);
        data.setPackageType(PackageTypeEnum.MIGRATION);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        assertFalse(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(InformationPackageTypeEnum.MIGRATION, response.getInformationPackageType());
        assertEquals(bagDir, response.getBagLocation());
        assertEquals( Set.of("2.6.2", "3.2.2", "3.2.3", "4.1"), getViolatedRuleNumbers(response));
    }

    @Test
    void validateMultipartZipFile() throws Exception {
        String name = "zips/audiences.zip";
        var bagDir = getResourceUrl(name);

        var data = new ValidateCommandDto();
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);
        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE)
            .field("zip", bagDir.openStream(), MediaType.valueOf("application/zip"));

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOkDto.class);

        assertFalse(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(InformationPackageTypeEnum.DEPOSIT, response.getInformationPackageType());
        assertNull(response.getBagLocation());
        assertEquals( Set.of("4.1"), getViolatedRuleNumbers(response));
    }

    @Test
    void validateZipFile() throws Exception {
        var bagDir = getResourceUrl("zips/audiences.zip");

        var data = new ValidateCommandDto();
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);
        var response = EXT.target("/validate")
            .request()
            .post(Entity.entity(bagDir.openStream(), MediaType.valueOf("application/zip")), ValidateOkDto.class);

        assertTrue(response.getIsCompliant());
        assertEquals("1.0.0", response.getProfileVersion());
        assertEquals(InformationPackageTypeEnum.DEPOSIT, response.getInformationPackageType());
        assertNull(response.getBagLocation());
        assertEquals(0, response.getRuleViolations().size());
    }

    @Test
    void validateBagNotFoundInEmptyZipFile() throws Exception {
        var bagDir = getResourceUrl("zips/empty.zip");

        var data = new ValidateCommandDto();
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);
        var response = EXT.target("/validate")
            .request()
            .post(Entity.entity(bagDir.openStream(), MediaType.valueOf("application/zip")), Response.class);

        assertEquals(400, response.getStatus());
        assertEquals("java.io.ByteArrayInputStream", response.getEntity().toString().replaceAll("@.*","")); // TODO can't we return a more descriptive body?
    }

    @Test
    void validateZipFileAndGetTextResponse() throws Exception {
        var bagDir = getResourceUrl("zips/invalid-sha1.zip");

        var data = new ValidateCommandDto();
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);

        var response = EXT.target("/validate")
            .request()
            .header("accept", "text/plain")
            .post(Entity.entity(bagDir.openStream(), MediaType.valueOf("application/zip")), String.class);

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
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), Response.class);

        assertEquals(400, response.getStatus());
    }

    @Test
    void validateWithHasOrganizationalIdentifier() throws Exception {
        var bagDir = getResourceUrl("bags/bag-with-is-version-of").getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(bagDir);
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);

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
            + "              \"value\": \"u1:organizational-identifier\"\n"
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
            + "      \"assignee\": \"@user001\",\n"
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
            + "      \"assignee\": \"@user001\",\n"
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
        var bagDir = getResourceUrl("bags/bag-with-is-version-of").getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(bagDir);
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);

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
            + "      \"assignee\": \"@user001\",\n"
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
            + "      \"assignee\": \"@user001\",\n"
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

        assertEquals(Set.of("4.4(b)"), getViolatedRuleNumbers(response));
        assertFalse(response.getIsCompliant());
        assertEquals("bag-with-is-version-of", response.getName());
    }

    @Test
    void validateWithNoMatchingSwordToken() throws Exception {
        var bagDir = getResourceUrl("bags/bag-with-is-version-of").getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(bagDir);
        data.setPackageType(PackageTypeEnum.DEPOSIT);
        data.setLevel(LevelEnum.WITH_DATA_STATION_CONTEXT);

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

        assertEquals(Set.of("4.2", "4.4(a)"), getViolatedRuleNumbers(response));
        assertFalse(response.getIsCompliant());
        assertEquals("bag-with-is-version-of", response.getName());
    }
}
