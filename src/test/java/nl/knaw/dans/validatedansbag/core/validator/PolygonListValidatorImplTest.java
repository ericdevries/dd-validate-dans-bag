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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolygonListValidatorImplTest {

    @Test
    void validateValid() {
        var msg = "1 2 3 4 5 6 7 8 1 2";
        assertDoesNotThrow(() -> new PolygonListValidatorImpl().validatePolygonList(msg));
    }

    @Test
    void validateTooShort() {
        var msg = "1 2 1 2";
        var result = new PolygonListValidatorImpl().validatePolygonList(msg);
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("too few values"));
    }

    @Test
    void validateUnEvenLength() {
        var msg = "1 2 3 4 5 6 7 1 2";
        var result = new PolygonListValidatorImpl().validatePolygonList(msg);
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("odd number of values"));
    }

    @Test
    void validateEndingDoesntEqualBegin() {
        var msg = "1 2 3 4 5 6 7 8";
        var result = new PolygonListValidatorImpl().validatePolygonList(msg);
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("unequal first and last"));
    }

    @Test
    void validateEvenSize() {
        var data = new String[] { "1", "2", "3", "4" };
        assertDoesNotThrow(() -> new PolygonListValidatorImpl().validateEvenSize(data));
    }

    @Test
    void validateUnEvenSize() {
        var data = new String[] { "1", "2", "3", "4", "5" };
        assertThrows(PolygonListValidator.PolygonValidationException.class, () -> new PolygonListValidatorImpl().validateEvenSize(data));
    }

    @Test
    void validateMinLength() {
        var data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8" };
        assertDoesNotThrow(() -> new PolygonListValidatorImpl().validateMinLength(data));
    }

    @Test
    void validateMinLengthIsTooLow() {
        var data = new String[] { "2", "3", "4", "5", "6", "7", "8" };
        assertThrows(PolygonListValidator.PolygonValidationException.class, () -> new PolygonListValidatorImpl().validateMinLength(data));
    }

    @Test
    void validateEndEqualsBegin() {
        var data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "1", "2" };
        assertDoesNotThrow(() -> new PolygonListValidatorImpl().validateEndEqualsBegin(data));
    }

    @Test
    void validateEndDoesNotEqualsBegin() {
        var data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8" };
        assertThrows(PolygonListValidator.PolygonValidationException.class, () -> new PolygonListValidatorImpl().validateEndEqualsBegin(data));
    }
}