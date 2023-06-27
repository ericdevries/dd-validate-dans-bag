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
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OptionalOriginalFilePathsIsCompleteTest extends RuleTestFixture {

    @Test
    void should_return_SUCCESS_when_original_filepaths_is_complete() throws Exception {
        Mockito.when(originalFilepathsService.exists(Mockito.any())).thenReturn(true);
        Mockito.when(filesXmlService.readFilepaths(Mockito.any()))
                .thenReturn(Stream.of(
                        Path.of("data/1.txt"),
                        Path.of("data/2.txt")
                ));

        Mockito.when(fileService.getAllFiles(Mockito.any()))
                .thenReturn(List.of(
                        Path.of("bagdir/data/a.txt"),
                        Path.of("bagdir/data/b.txt")
                ));

        Mockito.when(originalFilepathsService.getMapping(Mockito.any()))
                .thenReturn(List.of(
                        new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/1.txt"), Path.of("data/a.txt")),
                        new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/2.txt"), Path.of("data/b.txt"))
                ));

        var result = new OptionalOriginalFilePathsIsComplete(originalFilepathsService, fileService, filesXmlService).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_ERROR_when_mapping_references_a_file_that_is_not_present_in_the_bag() throws Exception {
        Mockito.when(originalFilepathsService.exists(Mockito.any())).thenReturn(true);
        Mockito.when(filesXmlService.readFilepaths(Mockito.any()))
                .thenReturn(Stream.of(
                        Path.of("data/1.txt"),
                        Path.of("data/2.txt")
                ));

        Mockito.when(fileService.getAllFiles(Mockito.any()))
                .thenReturn(List.of(
                        Path.of("bagdir/data/a.txt"),
                        Path.of("bagdir/data/b.txt")
                ));

        Mockito.when(originalFilepathsService.getMapping(Mockito.any()))
                .thenReturn(List.of(
                        new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/1.txt"), Path.of("data/a.txt")),
                        new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/2.txt"), Path.of("data/c.txt")) // this one is wrong
                ));

        var result = new OptionalOriginalFilePathsIsComplete(originalFilepathsService, fileService, filesXmlService).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_ERROR_when_mapping_is_incomplete() throws Exception {
        Mockito.when(originalFilepathsService.exists(Mockito.any())).thenReturn(true);
        Mockito.when(filesXmlService.readFilepaths(Mockito.any()))
                .thenReturn(Stream.of(
                        Path.of("data/1.txt"),
                        Path.of("data/2.txt")
                ));

        Mockito.when(fileService.getAllFiles(Mockito.any()))
                .thenReturn(List.of(
                        Path.of("bagdir/data/a.txt"),
                        Path.of("bagdir/data/b.txt")
                ));

        Mockito.when(originalFilepathsService.getMapping(Mockito.any()))
                .thenReturn(List.of(
                        // Mapping 1 -> a is missing
                        new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/2.txt"), Path.of("data/b.txt"))
                ));

        var result = new OptionalOriginalFilePathsIsComplete(originalFilepathsService, fileService, filesXmlService).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_ERROR_when_mappings_for_non_existent_files_present() throws Exception {
        Mockito.when(originalFilepathsService.exists(Mockito.any())).thenReturn(true);
        Mockito.when(filesXmlService.readFilepaths(Mockito.any()))
                .thenReturn(Stream.of(
                        Path.of("data/1.txt")
                ));

        Mockito.when(fileService.getAllFiles(Mockito.any()))
                .thenReturn(List.of(
                        Path.of("bagdir/data/a.txt")
                ));

        Mockito.when(originalFilepathsService.getMapping(Mockito.any()))
                .thenReturn(List.of(
                        new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/1.txt"), Path.of("data/a.txt")),
                        new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/2.txt"), Path.of("data/b.txt"))
                ));

        var result = new OptionalOriginalFilePathsIsComplete(originalFilepathsService, fileService, filesXmlService).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_SKIP_DEPENDENCIES_when_no_original_filepaths_present() throws Exception {
        Mockito.when(originalFilepathsService.exists(Mockito.any())).thenReturn(false);
        var result = new OptionalOriginalFilePathsIsComplete(originalFilepathsService, fileService, filesXmlService).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SKIP_DEPENDENCIES, result.getStatus());
    }
}
