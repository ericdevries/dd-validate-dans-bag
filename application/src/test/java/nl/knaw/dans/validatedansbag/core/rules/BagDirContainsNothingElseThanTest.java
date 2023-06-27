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

public class BagDirContainsNothingElseThanTest extends RuleTestFixture {
    @Test
    void should_return_SUCCESS_when_subset_of_allowed_files_found() throws Exception {
        var basePath = Path.of("bagdir/metadata");

        Mockito.when(fileService.getAllFilesAndDirectories(Mockito.eq(basePath)))
                .thenReturn(List.of(basePath.resolve("1.txt"), basePath.resolve("2.txt")));

        var result = new BagDirContainsNothingElseThan(Path.of("metadata"), new String[]{
                "1.txt",
                "2.txt",
                "3.txt"
        }, fileService).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_SUCCESS_when_exact_set_of_allowed_files_found() throws Exception {
        var basePath = Path.of("bagdir/metadata");

        Mockito.when(fileService.getAllFilesAndDirectories(Mockito.eq(basePath)))
                .thenReturn(List.of(basePath.resolve("1.txt"),
                        basePath.resolve("2.txt"), basePath.resolve("3.txt")));

        var result = new BagDirContainsNothingElseThan(Path.of("metadata"), new String[]{
                "1.txt",
                "2.txt",
                "3.txt"
        }, fileService).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_ERROR_when_other_than_allowed_files_found() throws Exception {
        var basePath = Path.of("bagdir/metadata");

        Mockito.when(fileService.getAllFilesAndDirectories(Mockito.eq(basePath)))
                .thenReturn(List.of(basePath.resolve("1.txt"), basePath.resolve("2.txt"),
                        basePath.resolve("oh no.txt")));

        var result = new BagDirContainsNothingElseThan(Path.of("metadata"), new String[]{
                "1.txt",
                "2.txt",
                "3.txt"
        }, fileService).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

}
