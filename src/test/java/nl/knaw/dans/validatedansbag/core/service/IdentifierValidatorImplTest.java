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
package nl.knaw.dans.validatedansbag.core.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierValidatorImplTest {

    @Test
    void validateDai() {
        var c = new IdentifierValidatorImpl();
        assertTrue(c.validateDai("12345678900"));
        assertTrue(c.validateDai("124398545"));
        assertTrue(c.validateDai("943582342"));
        assertTrue(c.validateDai("123489547"));
        assertTrue(c.validateDai("98534424"));
        assertTrue(c.validateDai("68391218"));

        // invalid strings
        assertFalse(c.validateDai("12345678901"));
        assertFalse(c.validateDai("124398546"));
        assertFalse(c.validateDai("943582343"));
        assertFalse(c.validateDai("123489548"));
        assertFalse(c.validateDai("98534425"));
        assertFalse(c.validateDai("6839121X"));
    }

    @Test
    void validateOrcid() {

        var validIds = List.of(
            "https://orcid.org/0000-0003-1415-9269",
            "https://orcid.org/0000-0002-1825-0097",
            "http://orcid.org/0000-0002-1825-0097"
        );

        var invalidIds = List.of(
            "https://orcid.org/0000-0003-1415-926X",
            "https://orcid.org/0002-1825-0097",
            "https://dans.knaw.nl/0000-0002-1825-0097"
        );

        var c = new IdentifierValidatorImpl();

        for (var id: validIds) {
            assertTrue(c.validateOrcid(id));
        }

        for (var id: invalidIds) {
            assertFalse(c.validateOrcid(id));
        }
    }


    @Test
    void validatorIsni() {
        var validIds = List.of(
            "0000000114559647",
            "https://isni.org/isni/0000-0002-1825-0097",
            "http://isni.org/isni/0000-0002-1825-0097",
            "https://www.isni.org/isni/0000-0002-1825-0097",
            "http://www.isni.org/isni/0000-0002-1825-0097",
            "0000 0001 2281 955X"
        );

        var invalidIds = List.of(
            "1234",
            "https://isni.org/isni/0000-0002-1825-0098"
        );

        var c = new IdentifierValidatorImpl();

        for (var id: validIds) {
            assertTrue(c.validateIsni(id));
        }

        for (var id: invalidIds) {
            assertFalse(c.validateIsni(id));
        }
    }
}