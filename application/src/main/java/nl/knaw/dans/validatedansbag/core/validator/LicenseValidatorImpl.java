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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class LicenseValidatorImpl implements LicenseValidator {
    private static final Logger log = LoggerFactory.getLogger(LicenseValidatorImpl.class);

    public LicenseValidatorImpl() {
    }

    @Override
    public boolean isValidLicense(String license) {
        try {
            new URI(license);
            return true;
        }
        catch (URISyntaxException e) {
            log.error("URI syntax error for uri {}", license, e);
            return false;
        }
    }
}
