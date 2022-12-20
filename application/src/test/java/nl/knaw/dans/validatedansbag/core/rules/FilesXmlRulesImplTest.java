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
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.FilesXmlService;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsService;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesXmlRulesImplTest {

    final FileService fileService = Mockito.mock(FileService.class);
    final FilesXmlService filesXmlService = Mockito.mock(FilesXmlService.class);
    final OriginalFilepathsService originalFilepathsService = Mockito.mock(OriginalFilepathsService.class);

    @AfterEach
    void afterEach() {
        Mockito.reset(fileService);
        Mockito.reset(originalFilepathsService);
    }

    private Document parseXmlString(String str) throws ParserConfigurationException, IOException, SAXException {
        return new XmlReaderImpl().readXmlString(str);
    }

    @Test
    void filesXmlFilePathAttributesContainLocalBagPathAndNonPayloadFilesAreNotDescribed() throws Exception {
        var checker = Mockito.spy(new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService));
        Mockito.doReturn(Set.of()).when(checker).filesXmlDescribesOnlyPayloadFiles(Mockito.any());

        var result = checker.filesXmlFilePathAttributesContainLocalBagPathAndNonPayloadFilesAreNotDescribed().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void filesXmlFilePathAttributesContainLocalBagPathAndNonPayloadFilesAreNotDescribedThrowsDoubleError() throws Exception {
        var checker = Mockito.spy(new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService));
        Mockito.doReturn(Set.of(Path.of("some/path.txt"))).when(checker).filesXmlDescribesOnlyPayloadFiles(Mockito.any());

        var result = checker.filesXmlFilePathAttributesContainLocalBagPathAndNonPayloadFilesAreNotDescribed().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void filesXmlNoDuplicateFilesAndEveryPayloadFileIsDescribed() throws Exception {
        var checker = Mockito.spy(new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService));
        Mockito.doReturn(Set.of()).when(checker).filesXmlNoDuplicates(Mockito.any());
        Mockito.doReturn(Set.of()).when(checker).filesXmlDescribesAllPayloadFiles(Mockito.any());

        var result = checker.filesXmlNoDuplicateFilesAndEveryPayloadFileIsDescribed().validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void filesXmlNoDuplicateFilesAndEveryPayloadFileIsDescribedThrowsDoubleError() throws Exception {
        var checker = Mockito.spy(new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService));
        Mockito.doReturn(Set.of(Path.of("broken"))).when(checker).filesXmlNoDuplicates(Mockito.any());
        Mockito.doReturn(Set.of(Path.of("another"))).when(checker).filesXmlDescribesAllPayloadFiles(Mockito.any());

        var result = checker.filesXmlNoDuplicateFilesAndEveryPayloadFileIsDescribed().validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertEquals(2, result.getErrorMessages().size());
    }

    @Test
    void filesXmlDescribesOnlyPayloadFiles() throws Exception {
        var fromFilesXml = Stream.of(
            Path.of("data/random images/image01.png")
        );

        Mockito.doReturn(fromFilesXml).when(filesXmlService).readFilepaths(Mockito.any());

        // even though image02 is not defined in the files.xml, this partial rule does not check for that
        var files = List.of(
            Path.of("bagdir/data/random images/image01.png"),
            Path.of("bagdir/data/random images/image02.png")
        );

        Mockito.doReturn(files).when(fileService).getAllFiles(Mockito.any());

        var checker = new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService);
        var result = checker.filesXmlDescribesOnlyPayloadFiles(Path.of("bagdir"));

        assertEquals(0, result.size());
    }

    @Test
    void filesXmlDescribesMoreThanOnlyPayloadFiles() throws Exception {
        var fromFilesXml = Stream.of(
            Path.of("data/random images/image01.png"),
            Path.of("data/random images/image02.png")
        );

        Mockito.doReturn(fromFilesXml).when(filesXmlService).readFilepaths(Mockito.any());

        var files = List.of(
            Path.of("bagdir/data/random images/image01.png")
        );

        Mockito.doReturn(files).when(fileService).getAllFiles(Mockito.any());

        var checker = new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService);
        var result = checker.filesXmlDescribesOnlyPayloadFiles(Path.of("bagdir"));

        assertEquals(1, result.size());
    }

    @Test
    void filesXmlNoDuplicates() throws Exception {

        var fromFilesXml = Stream.of(
            Path.of("data/random images/image01.png"),
            Path.of("data/random images/image02.png"),
            Path.of("data/random images/image03.png")
        );

        Mockito.doReturn(fromFilesXml).when(filesXmlService).readFilepaths(Mockito.any());

        var checker = new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService);

        var result = checker.filesXmlNoDuplicates(Path.of("bagdir"));
        assertEquals(0, result.size());
    }

    @Test
    void filesXmlNoDuplicatesButThereAreDuplicates() throws Exception {

        var fromFilesXml = Stream.of(
            Path.of("data/random images/image01.png"),
            Path.of("data/random images/image02.png"),
            Path.of("data/random images/image02.png")
        );

        Mockito.doReturn(fromFilesXml).when(filesXmlService).readFilepaths(Mockito.any());
        var checker = new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService);
        var result = checker.filesXmlNoDuplicates(Path.of("bagdir"));
        assertEquals(1, result.size());
    }

    @Test
    void filesXmlDescribesAllPayloadFiles() throws Exception {
        var fromFilesXml = Stream.of(
            Path.of("data/random images/image01.png"),
            Path.of("data/random images/image02.png"),
            Path.of("data/random images/image03.png")
        );

        Mockito.doReturn(fromFilesXml).when(filesXmlService).readFilepaths(Mockito.any());

        var files = List.of(
            Path.of("bagdir/data/random images/image01.png"),
            Path.of("bagdir/data/random images/image02.png"),
            Path.of("bagdir/data/random images/image03.png")
        );

        Mockito.doReturn(files).when(fileService).getAllFiles(Mockito.any());

        var checker = new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService);
        var result = checker.filesXmlDescribesAllPayloadFiles(Path.of("bagdir"));

        assertEquals(0, result.size());
    }

    @Test
    void filesXmlDescribesAllPayloadFilesButMissesOne() throws Exception {

        var fromFilesXml = Stream.of(
            Path.of("data/random images/image01.png"),
            Path.of("data/random images/image02.png"),
            Path.of("data/random images/image03.png")
        );

        Mockito.doReturn(fromFilesXml).when(filesXmlService).readFilepaths(Mockito.any());
        var files = List.of(
            Path.of("bagdir/data/random images/image01.png"),
            Path.of("bagdir/data/random images/image02.png"),
            Path.of("bagdir/data/random images/image03.png"),
            Path.of("bagdir/data/random images/image04.png")
        );

        Mockito.doReturn(files).when(fileService).getAllFiles(Mockito.any());

        var checker = new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService);
        var result = checker.filesXmlDescribesAllPayloadFiles(Path.of("bagdir"));

        assertEquals(1, result.size());
        assertTrue(result.stream().findAny().get().toString().contains("image04.png"));
    }
}