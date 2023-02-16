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
package nl.knaw.dans.validatedansbag.resources;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import nl.knaw.dans.validatedansbag.api.ValidateCommand;
import nl.knaw.dans.validatedansbag.api.ValidateOk;
import nl.knaw.dans.validatedansbag.api.ValidateOk.InformationPackageTypeEnum;
import nl.knaw.dans.validatedansbag.core.BagNotFoundException;
import nl.knaw.dans.validatedansbag.core.auth.SwordUser;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.RuleEngineService;
import nl.knaw.dans.validatedansbag.resources.util.MockAuthorization;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
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

import static nl.knaw.dans.validatedansbag.resources.util.TestUtil.basicUsernamePassword;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
class ValidateResourceTest {
    private final RuleEngineService ruleEngineService = Mockito.mock(RuleEngineService.class);
    private final FileService fileService = Mockito.mock(FileService.class);
    public final ResourceExtension EXT = ResourceExtension.builder()
        .addProvider(MultiPartFeature.class)
        .addProvider(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<SwordUser>()
            .setAuthenticator(new MockAuthorization())
            .setRealm("DANS")
            .buildAuthFilter()))
        .addResource(new ValidateResource(ruleEngineService, fileService))
        .addProvider(new AuthValueFactoryProvider.Binder<>(SwordUser.class))
        .build();

    @BeforeEach
    void setup() {
        Mockito.reset(fileService);
        Mockito.reset(ruleEngineService);
    }

    @Test
    void validateFormData_should_have_no_interactions_with_fileService_and_match_properties() {
        var data = new ValidateCommand();
        data.setBagLocation("it/is/here");
        data.setPackageType(ValidateCommand.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOk.class);

        Mockito.verifyNoInteractions(fileService);

        assertEquals("it/is/here", response.getBagLocation());
        assertEquals("here", response.getName());
        assertEquals(InformationPackageTypeEnum.DEPOSIT, response.getInformationPackageType());
    }

    @Test
    void validateFormData_should_create_zipFile_on_disk() throws Exception {
        var data = new ValidateCommand();
        data.setBagLocation(null);
        data.setPackageType(ValidateCommand.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE)
            .field("zip", new ByteArrayInputStream(new byte[4]), MediaType.valueOf("application/zip"));

        Mockito.doReturn(Path.of("/tmp/bag-1"))
            .when(fileService)
            .extractZipFile(Mockito.any(InputStream.class));

        Mockito.doReturn(Optional.of(Path.of("bagdir")))
            .when(fileService)
            .getFirstDirectory(Mockito.any());

        var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), ValidateOk.class);

        Mockito.verify(fileService).extractZipFile(Mockito.any(InputStream.class));

        assertEquals("bagdir", response.getName());
    }

    @Test
    void validateFormData_should_return_400_when_file_does_not_exist() throws Exception {
        var data = new ValidateCommand();
        data.setBagLocation("some/path");
        data.setPackageType(ValidateCommand.PackageTypeEnum.DEPOSIT);

        var multipart = new FormDataMultiPart()
            .field("command", data, MediaType.APPLICATION_JSON_TYPE);

        Mockito.doThrow(BagNotFoundException.class)
            .when(ruleEngineService)
            .validateBag(Mockito.any(), Mockito.any());

        try (var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .post(Entity.entity(multipart, multipart.getMediaType()), Response.class)) {

            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void validateZipFile_should_interact_with_fileService() throws Exception {
        var zip = Entity.entity(new ByteArrayInputStream(new byte[4]), MediaType.valueOf("application/zip"));

        Mockito.doReturn(Path.of("/tmp/bag-1"))
            .when(fileService)
            .extractZipFile(Mockito.any(InputStream.class));

        Mockito.doReturn(Optional.of(Path.of("bagdir")))
            .when(fileService)
            .getFirstDirectory(Mockito.any());

        var response = EXT.target("/validate")
            .request()
            .header("Authorization", basicUsernamePassword("user001", "user001"))
            .post(zip, ValidateOk.class);

        Mockito.verify(fileService).extractZipFile(Mockito.any(InputStream.class));

        assertEquals("bagdir", response.getName());
    }

    @Test
    void validateZipFile_should_return_401_with_wrong_credentials() throws Exception {
        var zip = Entity.entity(new ByteArrayInputStream(new byte[4]), MediaType.valueOf("application/zip"));

        try (var response = EXT.target("/validate")
            .request()
            .header("Authorization", basicUsernamePassword("unknown", "unknown"))
            .post(zip, Response.class)) {

            assertEquals(401, response.getStatus());
        }
    }

    @Test
    void validateZipFile_should_return_401_with_missing_credentials() throws Exception {
        var zip = Entity.entity(new ByteArrayInputStream(new byte[4]), MediaType.valueOf("application/zip"));

        try (var response = EXT.target("/validate")
            .request()
            .post(zip, Response.class)) {

            assertEquals(401, response.getStatus());
        }
    }

    @Test
    void validateZipFile_should_return_500_when_file_is_invalid() throws Exception {
        var zip = Entity.entity(new ByteArrayInputStream(new byte[4]), MediaType.valueOf("application/zip"));

        Mockito.doThrow(ZipError.class)
            .when(fileService)
            .extractZipFile(Mockito.any(InputStream.class));

        try (var response = EXT.target("/validate")
            .register(MultiPartFeature.class)
            .request()
            .header("Authorization", basicUsernamePassword("user001", "user001"))
            .post(zip, Response.class)) {

            assertEquals(500, response.getStatus());
        }
    }
}