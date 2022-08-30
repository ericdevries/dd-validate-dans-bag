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
import nl.knaw.dans.openapi.api.ValidateCommandDto;
import nl.knaw.dans.openapi.api.ValidateOkDto;
import nl.knaw.dans.validatedansbag.core.engine.RuleEngineImpl;
import nl.knaw.dans.validatedansbag.core.rules.BagRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.DatastationRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.FilesXmlRulesImpl;
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
import java.net.MalformedURLException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
class ValidateResourceImplTest {
    public static final ResourceExtension EXT;

    static {
        try {
            EXT = ResourceExtension.builder()
                .addProvider(MultiPartFeature.class)
                .addProvider(ValidateOkDtoYamlMessageBodyWriter.class)
                .addResource(buildValidateResource())
                .build();
        }
        catch (MalformedURLException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    static ValidateResource buildValidateResource() throws MalformedURLException, SAXException {

        var fileService = new FileServiceImpl();
        var bagItMetadataReader = new BagItMetadataReaderImpl();
        var xmlReader = new XmlReaderImpl();
        var daiDigestCalculator = new IdentifierValidatorImpl();
        var polygonListValidator = new PolygonListValidatorImpl();
        var originalFilepathsService = new OriginalFilepathsServiceImpl(fileService);
        var licenseValidator = new LicenseValidatorImpl();

        //        var xmlSchemaValidator = new XmlSchemaValidatorImpl();
        // the schema validator loads external schema's which is really slow
        var xmlSchemaValidator = Mockito.mock(XmlSchemaValidator.class);

        var dataverseService = Mockito.mock(DataverseService.class);

        // set up the different rule implementations
        var bagRules = new BagRulesImpl(fileService, bagItMetadataReader, xmlReader, originalFilepathsService, dataverseService, daiDigestCalculator, polygonListValidator, licenseValidator);
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
    }

    @Test
    void validateFormDataWithInvalidBag() throws Exception {
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
        assertEquals(ValidateOkDto.InfoPackageTypeEnum.DEPOSIT, response.getInfoPackageType());
        assertEquals(filename, response.getBagLocation());
        assertEquals(1, response.getRuleViolations().size());
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
        assertEquals(ValidateOkDto.InfoPackageTypeEnum.DEPOSIT, response.getInfoPackageType());
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
        assertEquals(ValidateOkDto.InfoPackageTypeEnum.DEPOSIT, response.getInfoPackageType());
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

        assertTrue(response.contains("bagLocation:"));
        assertTrue(response.contains("ruleViolations:"));
    }

    @Test
    void validateMultipartDataLocationCouldNotBeRead() throws Exception {
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
}