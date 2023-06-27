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

import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.license.License;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.stream.Collectors;

public class LicenseValidatorImpl implements LicenseValidator {
    private static final Logger log = LoggerFactory.getLogger(LicenseValidatorImpl.class);

    private final DataverseService dataverseService;

    public LicenseValidatorImpl(DataverseService dataverseService) {
        this.dataverseService = dataverseService;
    }

    @Override
    public boolean isValidUri(String license) {
        try {
            new URI(license);
            return true;
        }
        catch (URISyntaxException e) {
            log.error("URI syntax error for uri {}", license, e);
            return false;
        }
    }

    @Override
    public boolean isValidLicense(String license) throws IOException, DataverseException {
        // strip trailing slashes so urls are more consistent
        // it might be worth investigating if this should be more extensive
        // for example, also dropping the www. prefix
        var licenses = dataverseService.getLicenses().stream()
            .filter(License::isActive)
            .map(License::getUri)
            .filter(Objects::nonNull)
            .map(this::normalizeLicense)
            .collect(Collectors.toSet());

        var normalizedLicense = normalizeLicense(license);
        log.trace("Normalized license from {} to {}", license, normalizedLicense);

        return licenses.contains(normalizedLicense);
    }

    String normalizeLicense(String license) {
        return license.replaceAll("/+$", "");
    }
}
