package nl.knaw.dans.validatedansbag.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DaiDigestCalculatorImplTest {

    @Test
    void calculateChecksum() {
        var c = new DaiDigestCalculatorImpl();
        assertEquals('0', c.calculateChecksum("1234567890"));
        assertEquals('5', c.calculateChecksum("12439854"));
        assertEquals('2', c.calculateChecksum("94358234"));
        assertEquals('7', c.calculateChecksum("12348954"));
        assertEquals('4', c.calculateChecksum("9853442"));
        assertEquals('8', c.calculateChecksum("6839121"));
    }
}