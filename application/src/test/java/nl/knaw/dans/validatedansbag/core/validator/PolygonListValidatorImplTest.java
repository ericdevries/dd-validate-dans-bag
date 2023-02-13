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
    void validatePolygonList_should_not_throw_with_even_number() {
        // this is an even number of numbers
        var msg = "1 2 3 4 5 6 7 8 1 2";
        assertDoesNotThrow(() -> new PolygonListValidatorImpl().validatePolygonList(msg));
    }

    @Test
    void validatePolygonList_should_not_validate_with_less_than_8_items() {
        var msg = "1 2 1 2";
        var result = new PolygonListValidatorImpl().validatePolygonList(msg);
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("too few values"));
    }

    @Test
    void validatePolygonList_should_not_validate_with_odd_number() {
        // this is an odd number of numbers
        var msg = "1 2 3 4 5 6 7 1 2";
        var result = new PolygonListValidatorImpl().validatePolygonList(msg);
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("odd number of values"));
    }

    @Test
    void validatePolygonList_should_not_validate_if_last_2_numbers_dont_match_first_2_numbers() {
        var msg = "1 2 3 4 5 6 7 8";
        var result = new PolygonListValidatorImpl().validatePolygonList(msg);
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("unequal first and last"));
    }

    @Test
    void validateEvenSize_should_not_throw_with_even_number() {
        var data = new String[] { "1", "2", "3", "4" };
        assertDoesNotThrow(() -> new PolygonListValidatorImpl().validateEvenSize(data));
    }

    @Test
    void validateEvenSize_should_throw_with_uneven_number() {
        var data = new String[] { "1", "2", "3", "4", "5" };
        assertThrows(PolygonListValidator.PolygonValidationException.class, () -> new PolygonListValidatorImpl().validateEvenSize(data));
    }

    @Test
    void validateMinLength_should_not_throw_with_8_parameters() {
        var data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8" };
        assertDoesNotThrow(() -> new PolygonListValidatorImpl().validateMinLength(data));
    }

    @Test
    void validateMinLength_should_throw_with_7_parameters() {
        var data = new String[] { "2", "3", "4", "5", "6", "7", "8" };
        assertThrows(PolygonListValidator.PolygonValidationException.class, () -> new PolygonListValidatorImpl().validateMinLength(data));
    }

    @Test
    void validateEndEqualsBegin_should_not_throw_if_first_2_items_match_last_2_items() {
        var data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "1", "2" };
        assertDoesNotThrow(() -> new PolygonListValidatorImpl().validateEndEqualsBegin(data));
    }

    @Test
    void validateEndEqualsBegin_should_throw_if_first_2_items_do_not_match_last_2_items() {
        var data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8" };
        assertThrows(PolygonListValidator.PolygonValidationException.class, () -> new PolygonListValidatorImpl().validateEndEqualsBegin(data));
    }
}