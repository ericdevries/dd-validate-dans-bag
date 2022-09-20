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

import java.util.Arrays;
import java.util.stream.Collectors;

public class PolygonListValidatorImpl implements PolygonListValidator {
    @Override
    public PolygonValidationResult validatePolygonList(String polygons) {
        var parts = polygons.split("\\s+");

        try {
            // each of these will throw an error if something is wrong
            validateEvenSize(parts);
            validateMinLength(parts);
            validateEndEqualsBegin(parts);
        }
        catch (PolygonValidationException e) {
            return PolygonValidationResult.invalid(e.getMessage());
        }

        return PolygonValidationResult.valid();
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
