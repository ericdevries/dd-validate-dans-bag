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

import gov.loc.repository.bagit.domain.Bag;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BagInfoExistsAndIsWellformedTest extends RuleTestFixture {
    @Test
    void should_return_SUCCESS_if_bag_if_found_and_can_be_parsed_by_baglib() throws Exception {
        Mockito.when(fileService.isFile(Mockito.any()))
                .thenReturn(true);

        Mockito.when(bagItMetadataReader.getBag(Mockito.any()))
                .thenReturn(Optional.of(new Bag()));

        var result = new BagInfoExistsAndIsWellformed(bagItMetadataReader, fileService).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_ERROR_if_bag_not_found() throws Exception {
        Mockito.when(fileService.isFile(Mockito.any()))
                .thenReturn(false);

        var result = new BagInfoExistsAndIsWellformed(bagItMetadataReader, fileService).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_ERROR_if_bag_found_but_cannot_be_parsed_by_baglib() throws Exception {
        Mockito.when(fileService.isFile(Mockito.any()))
                .thenReturn(true);

        Mockito.when(bagItMetadataReader.getBag(Mockito.any()))
                .thenReturn(Optional.empty());

        var result = new BagInfoExistsAndIsWellformed(bagItMetadataReader, fileService).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

}
