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
package nl.knaw.dans.validatedansbag.core.rules;

import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BagInfoCreatedElementIsIso8601DateTest extends RuleTestFixture {

    @Test
    void should_return_SUCCESS_if_a_valid_date_is_found() throws Exception {
        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.eq("Created")))
                .thenReturn("2022-01-01T01:23:45.678+00:00");

        var result = new BagInfoCreatedElementIsIso8601Date(bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_ERROR_if_T_separator_absent() throws Exception {
        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.eq("Created")))
                .thenReturn("2022-01-01 01:23:45.678");

        var result = new BagInfoCreatedElementIsIso8601Date(bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_ERROR_if_no_timezone_present() throws Exception {
        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.eq("Created")))
                .thenReturn("2022-01-01T01:23:45.678");

        var result = new BagInfoCreatedElementIsIso8601Date(bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_ERROR_if_no_millisecond_precision() throws Exception {
        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.eq("Created")))
                .thenReturn("2022-01-01T01:23:45+00:00");


        var result = new BagInfoCreatedElementIsIso8601Date(bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }
}


