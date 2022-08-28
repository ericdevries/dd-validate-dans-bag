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
import nl.knaw.dans.validatedansbag.core.BagNotFoundException;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.RuleEngineService;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipError;

@ExtendWith(DropwizardExtensionsSupport.class)
class ValidateResourceTest {
    private final RuleEngineService ruleEngineService = Mockito.mock(RuleEngineService.class);
    private final FileService fileService = Mockito.mock(FileService.class);
    public final ResourceExtension EXT = ResourceExtension.builder()
        .addProvider(MultiPartFeature.class)
        .addResource(new ValidateResource(ruleEngineService, fileService))
        .build();

    @BeforeEach
    void setup() {
        Mockito.reset(fileService);
        Mockito.reset(ruleEngineService);
    }

    @Test
    void validateFormData() throws Exception {
        var data = new ValidateCommandDto();
        data.setBagLocation("it/is/here");
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), String.class);

        Mockito.verifyNoInteractions(fileService);//.extractZipFile(Mockito.any(Path.class));
    }

    @Test
    void validateFormDataWithZipFile() throws Exception {
        var data = new ValidateCommandDto();
        data.setBagLocation(null);
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE)
            .field("zip", new ByteArrayInputStream(new byte[4]), MediaType.valueOf("application/zip"));

        Mockito.doReturn(Optional.of(Path.of("audiences")))
            .when(fileService).extractZipFile(Mockito.any(InputStream.class));

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), String.class);

        Mockito.verify(fileService).extractZipFile(Mockito.any(InputStream.class));//.extractZipFile(Mockito.any(Path.class));
    }

    @Test
    void validateZipFile() throws Exception {
        var zip = Entity.entity(new ByteArrayInputStream(new byte[4]), MediaType.valueOf("application/zip"));

        Mockito.doReturn(Optional.of(Path.of("audiences")))
            .when(fileService).extractZipFile(Mockito.any(InputStream.class));

        var response = EXT.target("/validate")
            .request()
            .post(zip, String.class);

        Mockito.verify(fileService).extractZipFile(Mockito.any(InputStream.class));//.extractZipFile(Mockito.any(Path.class));
    }

    @Test
    void validateMultipartFileButTheFileDoesNotExist() throws Exception {
        var data = new ValidateCommandDto();
        data.setBagLocation("some/path");
        data.setPackageType(ValidateCommandDto.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        Mockito.doThrow(BagNotFoundException.class)
            .when(ruleEngineService).validateBag(Mockito.any(), Mockito.any());

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), Response.class);

        Assertions.assertEquals(400, response.getStatus());
    }

    @Test
    void validateZipButItIsNotAValidZip() throws Exception {
        var zip = Entity.entity(new ByteArrayInputStream(new byte[4]), MediaType.valueOf("application/zip"));

        Mockito.doThrow(ZipError.class)
            .when(fileService).extractZipFile(Mockito.any(InputStream.class));

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(zip, Response.class);

        Assertions.assertEquals(500, response.getStatus());
    }
}