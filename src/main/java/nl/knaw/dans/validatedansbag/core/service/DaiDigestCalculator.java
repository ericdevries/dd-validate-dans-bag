package nl.knaw.dans.validatedansbag.core.service;

public interface DaiDigestCalculator {

    char calculateChecksum(String str, int modeMax);
}
