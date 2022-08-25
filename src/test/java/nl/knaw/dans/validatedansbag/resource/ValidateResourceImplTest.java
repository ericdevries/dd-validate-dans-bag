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
import nl.knaw.dans.openapi.api.ValidateJsonOkDto;
import nl.knaw.dans.validatedansbag.core.engine.RuleEngineImpl;
import nl.knaw.dans.validatedansbag.core.rules.BagRulesImpl;
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
import java.net.MalformedURLException;
import java.util.Objects;

@ExtendWith(DropwizardExtensionsSupport.class)
class ValidateResourceImplTest {
    public static final ResourceExtension EXT;

    static {
        try {
            EXT = ResourceExtension.builder()
                .addProvider(MultiPartFeature.class)
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

        // set up the engine and the service that has a default set of rules
        var ruleEngine = new RuleEngineImpl();
        var ruleEngineService = new RuleEngineServiceImpl(ruleEngine, bagRules, xmlRules, filesXmlRules);

        return new ValidateResource(ruleEngineService, fileService);
    }

    @BeforeEach
    void setup() {
        //        Mockito.reset(fileService);
        //        Mockito.reset(ruleEngineService);
    }

    @Test
    void validateFormData() throws Exception {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("bags/audiences")).getFile();

        var data = new ValidateCommandDto();
        data.setBagLocation(filename);
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);
        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateJsonOkDto.class);

        System.out.println("RESULT: " + response);
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
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateJsonOkDto.class);

        System.out.println("RESULT: " + response);
    }

    @Test
    void validateZipFile() throws Exception {
        var filename = Objects.requireNonNull(getClass().getClassLoader().getResource("zips/audiences.zip"));

        var data = new ValidateCommandDto();
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);
        var response = EXT.target("/validate")
            .request()
            .post(Entity.entity(filename.openStream(), MediaType.valueOf("application/zip")), ValidateJsonOkDto.class);
        //.post(InputStreamEntity.entity(multipart, multipart.getMediaType()), ValidateJsonOkDto.class);

        System.out.println("RESULT: " + response);
    }
}