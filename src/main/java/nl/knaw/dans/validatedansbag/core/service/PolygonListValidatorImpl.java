package nl.knaw.dans.validatedansbag.core.service;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PolygonListValidatorImpl implements PolygonListValidator {
    @Override
    public void validatePolygonList(String polygons) throws PolygonValidationException {
        var parts = polygons.split("\\s+");

        // each of these will throw an error if something is wrong
        validateEvenSize(parts);
        validateMinLength(parts);
        validateEndEqualsBegin(parts);
    }

    void validateEvenSize(String[] parts) throws PolygonValidationException {
        if (parts.length % 2 != 0) {
            throw new PolygonValidationException(String.format(
                "Found posList with odd number of values: %s. %s", parts.length, formatPosList(parts)
            ));
        }
    }

    void validateMinLength(String[] parts) throws PolygonValidationException {
        if (parts.length < 8) {
            throw new PolygonValidationException(String.format(
                "Found posList with too few values (fewer than 4 pairs). %s", formatPosList(parts)
            ));
        }
    }

    void validateEndEqualsBegin(String[] parts) throws PolygonValidationException {
        if (!parts[0].equals(parts[parts.length - 2]) || !parts[1].equals(parts[parts.length - 1])) {
            throw new PolygonValidationException(String.format(
                "Found posList with unequal first and last pairs. %s", formatPosList(parts)
            ));
        }
    }

    String formatPosList(String[] parts) {
        return String.format("(Offending posList starts with: %s...)",
            Arrays.stream(parts).limit(10).collect(Collectors.joining(", "))
        );
    }
}
