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

import java.net.URI;
import java.util.stream.Collectors;

public class LicenseValidatorImpl implements LicenseValidator {
    private final LicenseConfig licenseConfig;

    public LicenseValidatorImpl(LicenseConfig licenseConfig) {
        this.licenseConfig = licenseConfig;
    }

    String normalizeLicense(String license) {
        return license.replaceAll("/+$", "");
    }

    @Override
    public boolean isValidLicense(String license) {
        // strip trailing slashes so url's are more consistent
        var licenses = licenseConfig.getAllowedLicenses().stream()
            .map(URI::toString)
            .map(this::normalizeLicense)
            .collect(Collectors.toSet());

        return licenses.contains(normalizeLicense(license));
    }
}
