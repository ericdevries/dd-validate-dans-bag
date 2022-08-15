package nl.knaw.dans.validatedansbag.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LicenseValidatorImplTest {

    @Test
    void isValidLicense() {
        var license = "http://creativecommons.org/licenses/by-nc-nd/4.0/";
        assertTrue(new LicenseValidatorImpl().isValidLicense(license));
    }

    @Test
    void isValidLicenseWhenTrailingSlashIsMissing() {
        var license = "http://creativecommons.org/licenses/by-nc-nd/4.0";
        assertTrue(new LicenseValidatorImpl().isValidLicense(license));
    }
    @Test
    void isInvalidLicense() {
        var license = "http://creativecommons.org/licenses/by-nc-nd/4";
        assertFalse(new LicenseValidatorImpl().isValidLicense(license));
        assertFalse(new LicenseValidatorImpl().isValidLicense("something completely different"));
    }
}