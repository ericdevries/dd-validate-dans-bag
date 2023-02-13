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

import nl.knaw.dans.validatedansbag.core.config.LicenseConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseValidatorImplTest {

    final LicenseConfig config = new LicenseConfig();

    @Test
    void isValidLicense_should_return_true_if_it_is_found_in_license_config() {
        var license = "http://creativecommons.org/licenses/by-nc-nd/4.0/";
        config.setAllowedLicenses(List.of(URI.create(license)));
        assertTrue(new LicenseValidatorImpl(config).isValidLicense(license));
    }

    @Test
    void isValidLicense_should_return_true_with_trailing_slash_missing() {
        var license = "http://creativecommons.org/licenses/by-nc-nd/4.0";
        config.setAllowedLicenses(List.of(URI.create(license)));
        assertTrue(new LicenseValidatorImpl(config).isValidLicense(license));
    }

    @Test
    void isValidLicense_should_return_false_when_url_is_not_found_in_config() {
        var license = "http://creativecommons.org/licenses/by-nc-nd/4";
        var validLicense = "http://creativecommons.org/licenses/by-nc-nd/4.0";
        config.setAllowedLicenses(List.of(URI.create(validLicense)));
        assertFalse(new LicenseValidatorImpl(config).isValidLicense(license));
        assertFalse(new LicenseValidatorImpl(config).isValidLicense("something completely different"));
    }
}