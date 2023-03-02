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
package nl.knaw.dans.validatedansbag.core.validator;

import nl.knaw.dans.lib.dataverse.model.license.License;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseValidatorImplTest {

    final DataverseService dataverseService = Mockito.mock(DataverseService.class);

    @AfterEach
    void afterEach() {
        Mockito.reset(dataverseService);
    }

    @Test
    void isValidLicenseURI_should_return_true_if_license_is_valid_uri() {
        var license = "http://creativecommons.org/licenses/by-nc-nd/4.0/";
        assertTrue(new LicenseValidatorImpl(dataverseService).isValidLicenseURI(license));
    }

    @Test
    void isValidLicenseURI_should_return_false_when_url_is_not_valid() {
        var license = "invalid license";
        assertFalse(new LicenseValidatorImpl(dataverseService).isValidLicenseURI(license));
        assertFalse(new LicenseValidatorImpl(dataverseService).isValidLicenseURI("something completely different"));
    }

    @Test
    void isValidLicense_should_return_true_for_valid_uri() throws Exception {
        var license = "http://dans.nl/";
        var dvLicense = new License();
        dvLicense.setActive(true);
        dvLicense.setUri("http://dans.nl");

        Mockito.when(dataverseService.getLicenses())
            .thenReturn(List.of(dvLicense));

        assertTrue(new LicenseValidatorImpl(dataverseService).isValidLicense(license));
    }

    @Test
    void isValidLicense_should_return_false_for_invalid_uri() throws Exception {
        var license = "http://dans.nl/";
        var dvLicense = new License();
        dvLicense.setActive(true);
        dvLicense.setUri("http://something.else.com");

        Mockito.when(dataverseService.getLicenses())
            .thenReturn(List.of(dvLicense));

        assertFalse(new LicenseValidatorImpl(dataverseService).isValidLicense(license));
    }
}