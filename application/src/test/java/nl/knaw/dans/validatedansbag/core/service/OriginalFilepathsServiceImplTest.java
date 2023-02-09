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
package nl.knaw.dans.validatedansbag.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OriginalFilepathsServiceImplTest {
    final FileService fileService = Mockito.mock(FileService.class);

    @AfterEach
    void afterEach() {
        Mockito.reset(fileService);
    }

    @Test
    void getMapping_should_map_files_to_original_paths_based_on_txt() throws Exception {
        var contents = "data/12.txt data/leeg.txt\n"
            + "data/13.txt data/sub/leeg2.txt\n"
            + "data/14.txt data/sub/sub/vacio.txt\n";

        Mockito.when(fileService.readFileContents(Mockito.eq(Path.of("bagdir/original-filepaths.txt"))))
            .thenReturn(contents.getBytes(StandardCharsets.UTF_8));

        var service = new OriginalFilepathsServiceImpl(fileService);
        var result = service.getMapping(Path.of("bagdir"));

        var expected = Set.of(
            new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/leeg.txt"), Path.of("data/12.txt")),
            new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/sub/leeg2.txt"), Path.of("data/13.txt")),
            new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/sub/sub/vacio.txt"), Path.of("data/14.txt"))
        );

        assertEquals(expected, new HashSet<>(result));
    }

    @Test
    void getMapping_should_ignore_row_with_1_item() throws Exception {
        var contents = "data/12.txt data/leeg.txt\n"
            + "data/13.txt data/sub/leeg2.txt\n"
            + "singleitem\n";

        Mockito.when(fileService.readFileContents(Mockito.eq(Path.of("bagdir/original-filepaths.txt"))))
            .thenReturn(contents.getBytes(StandardCharsets.UTF_8));

        var service = new OriginalFilepathsServiceImpl(fileService);
        var result = service.getMapping(Path.of("bagdir"));

        var expected = Set.of(
            new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/leeg.txt"), Path.of("data/12.txt")),
            new OriginalFilepathsService.OriginalFilePathItem(Path.of("data/sub/leeg2.txt"), Path.of("data/13.txt"))
        );

        assertEquals(expected, new HashSet<>(result));
    }

    @Test
    void getMapping_should_return_empty_result_if_original_filepaths_txt_does_not_exist() throws Exception {
        Mockito.when(fileService.readFileContents(Mockito.eq(Path.of("bagdir/original-filepaths.txt"))))
            .thenThrow(new FileNotFoundException("file not found"));

        var service = new OriginalFilepathsServiceImpl(fileService);
        var result = service.getMapping(Path.of("bagdir"));

        assertEquals(0, result.size());
    }
}