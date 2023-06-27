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

import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BagIsValidTest extends RuleTestFixture {
    @Test
    void should_return_SUCCESS_on_valid_bag() throws Exception {
        var result = new BagIsValid(bagItMetadataReader).validate(Path.of("testpath"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());

        Mockito.verify(bagItMetadataReader).verifyBag(Path.of("testpath"));
    }

    @Test
    void should_return_ERROR_on_invalid_bag() throws Exception {
        Mockito.doThrow(new InvalidBagitFileFormatException("Invalid file format"))
                .when(bagItMetadataReader).verifyBag(Mockito.any());

        var result = new BagIsValid(bagItMetadataReader).validate(Path.of("testpath"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

}
