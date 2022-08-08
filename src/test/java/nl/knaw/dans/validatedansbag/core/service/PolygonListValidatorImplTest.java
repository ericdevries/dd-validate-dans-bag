package nl.knaw.dans.validatedansbag.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        var e = assertThrows(PolygonListValidator.PolygonValidationException.class, () -> new PolygonListValidatorImpl().validatePolygonList(msg));
        assertTrue(e.getLocalizedMessage().contains("too few values"));
    }

    @Test
    void validateUnEvenLength() {
        var msg = "1 2 3 4 5 6 7 1 2";
        var e = assertThrows(PolygonListValidator.PolygonValidationException.class, () -> new PolygonListValidatorImpl().validatePolygonList(msg));
        assertTrue(e.getLocalizedMessage().contains("odd number of values"));
    }

    @Test
    void validateEndingDoesntEqualBegin() {
        var msg = "1 2 3 4 5 6 7 8";
        var e = assertThrows(PolygonListValidator.PolygonValidationException.class, () -> new PolygonListValidatorImpl().validatePolygonList(msg));
        assertTrue(e.getLocalizedMessage().contains("unequal first and last"));
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