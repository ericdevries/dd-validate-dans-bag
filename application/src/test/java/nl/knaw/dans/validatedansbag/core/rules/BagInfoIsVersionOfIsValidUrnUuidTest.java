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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BagInfoIsVersionOfIsValidUrnUuidTest extends RuleTestFixture {
    @Test
    void should_return_SUCCESS_when_is_version_of_is_a_valid_urn_uuid() throws Exception {
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Is-Version-Of")))
                .thenReturn(List.of("urn:uuid:76cfdebf-e43d-4c56-a886-e8375c745429"));

        var result = new BagInfoIsVersionOfIsValidUrnUuid(bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_ERROR_when_not_a_urn() throws Exception {
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Is-Version-Of")))
                .thenReturn(List.of("http://google.com"));

        assertEquals(RuleResult.Status.ERROR, new BagInfoIsVersionOfIsValidUrnUuid(bagItMetadataReader).validate(Path.of("bagdir")).getStatus());
    }

    @Test
    void should_return_ERROR_when_urn_but_not_subscheme_uuid() throws Exception {
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Is-Version-Of")))
                .thenReturn(List.of("urn:notuuid:76cfdebf-e43d-4c56-a886-e8375c745429"));

        assertEquals(RuleResult.Status.ERROR, new BagInfoIsVersionOfIsValidUrnUuid(bagItMetadataReader).validate(Path.of("bagdir")).getStatus());
    }

    @Test
    void should_return_ERROR_when_urn_uuid_scheme_but_not_a_uuid_value() throws Exception {
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Is-Version-Of")))
                .thenReturn(List.of("urn:uuid:1234"));

        assertEquals(RuleResult.Status.ERROR, new BagInfoIsVersionOfIsValidUrnUuid(bagItMetadataReader).validate(Path.of("bagdir")).getStatus());
    }
}
