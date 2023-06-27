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

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OptionalBagFileIsUtf8DecodableTest extends RuleTestFixture {

    @Test
    void should_return_SUCCESS_when_file_is_successfully_read() throws Exception {
        Mockito.when(fileService.exists(Mockito.any())).thenReturn(true);
        Mockito.when(fileService.readFileContents(Mockito.any(), Mockito.any())).thenReturn(CharBuffer.allocate(1));

        var result = new OptionalBagFileIsUtf8Decodable(Path.of("somefile.txt"), fileService).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_SUCCESS_when_file_does_not_exist() throws Exception {
        Mockito.when(fileService.exists(Mockito.any())).thenReturn(false);

        var result = new OptionalBagFileIsUtf8Decodable(Path.of("somefile.txt"), fileService).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SKIP_DEPENDENCIES, result.getStatus());
    }

    @Test
    void should_return_ERROR_when_reading_file_throws_charencoding_exception() throws Exception {
        Mockito.when(fileService.exists(Mockito.any())).thenReturn(true);
        Mockito.when(fileService.readFileContents(Mockito.any(), Mockito.any()))
                .thenThrow(new CharacterCodingException());

        var result = new OptionalBagFileIsUtf8Decodable(Path.of("somefile.txt"), fileService).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }
}
