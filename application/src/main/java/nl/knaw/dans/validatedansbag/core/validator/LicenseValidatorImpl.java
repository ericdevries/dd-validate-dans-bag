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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.stream.Collectors;

public class LicenseValidatorImpl implements LicenseValidator {
    private static final Logger log = LoggerFactory.getLogger(LicenseValidatorImpl.class);

    private final LicenseConfig licenseConfig;

    public LicenseValidatorImpl(LicenseConfig licenseConfig) {
        this.licenseConfig = licenseConfig;
    }

    String normalizeLicense(String license) {
        return license.replaceAll("/+$", "");
    }

    @Override
    public boolean isValidLicense(String license) {
        // strip trailing slashes so urls are more consistent
        // it might be worth investigating if this should be more extensive
        // for example, also dropping the www. prefix
        var licenses = licenseConfig.getAllowedLicenses().stream()
            .map(URI::toString)
            .map(this::normalizeLicense)
            .collect(Collectors.toSet());

        var normalizedLicense = normalizeLicense(license);
        log.trace("Normalized license from {} to {}", license, normalizedLicense);

        return licenses.contains(normalizedLicense);
    }
}
