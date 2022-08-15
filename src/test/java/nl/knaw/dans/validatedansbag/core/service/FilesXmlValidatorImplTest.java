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

import nl.knaw.dans.validatedansbag.core.engine.RuleViolationDetailsException;
import nl.knaw.dans.validatedansbag.core.validator.FilesXmlValidatorImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesXmlValidatorImplTest {

    final FileService fileService = Mockito.mock(FileService.class);
    final XmlReader xmlReader = Mockito.spy(new XmlReaderImpl());
    final OriginalFilepathsService originalFilepathsService = Mockito.mock(OriginalFilepathsService.class);

    @AfterEach
    void afterEach() {
        Mockito.reset(fileService);
        Mockito.reset(xmlReader);
        Mockito.reset(originalFilepathsService);
    }

    private Document parseXmlString(String str) throws ParserConfigurationException, IOException, SAXException {
        return new XmlReaderImpl().readXmlString(str);
    }

    @Test
    void filesXmlHasDocumentElementFiles() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";

        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertDoesNotThrow(() -> checker.filesXmlHasDocumentElementFiles().validate(Path.of("bagdir")));

    }

    @Test
    void filesXmlDoesNotHaveDocumentElementFiles() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<notfiles xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "</notfiles>\n"
            + "\n";

        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        var e = assertThrows(RuleViolationDetailsException.class,
            () -> checker.filesXmlHasDocumentElementFiles().validate(Path.of("bagdir")));

    }

    @Test
    void filesXmlHasOnlyFiles() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";

        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertDoesNotThrow(() -> checker.filesXmlHasOnlyFiles().validate(Path.of("bagdir")));
    }

    @Test
    void filesXmlHasMoreThanOnlyFiles() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "    <path></path>\n"
            + "</files>\n"
            + "\n";

        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        var e = assertThrows(RuleViolationDetailsException.class,
            () -> checker.filesXmlHasOnlyFiles().validate(Path.of("bagdir")));

        assertTrue(e.getLocalizedMessage().contains("path"));
    }

    @Test
    void filesXmlFileElementsAllHaveFilepathAttribute() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";

        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertDoesNotThrow(() -> checker.filesXmlFileElementsAllHaveFilepathAttribute().validate(Path.of("bagdir")));
    }

    @Test
    void filesXmlFileElementsAllHaveFilepathAttributeButNotALl() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file>\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file filepath=\"\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";

        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        var e = assertThrows(RuleViolationDetailsException.class,
            () -> checker.filesXmlFileElementsAllHaveFilepathAttribute().validate(Path.of("bagdir")));

        assertTrue(e.getLocalizedMessage().startsWith("2"));
    }

    @Test
    void filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFiles() throws Exception {

        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image03.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";

        var document = parseXmlString(xml);

        var files = List.of(
            Path.of("bagdir/data/random images/image01.png"),
            Path.of("bagdir/data/random images/image02.png"),
            Path.of("bagdir/data/random images/image03.png")
        );

        Mockito.doReturn(files).when(fileService).getAllFiles(Mockito.any());
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertDoesNotThrow(() -> checker.filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFiles().validate(Path.of("bagdir")));
    }

    @Test
    void filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFilesWithNamespace() throws Exception {

        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns=\"http://easy.dans.knaw.nl/schemas/bag/metadata/files/\" xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image03.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";

        var document = parseXmlString(xml);

        var files = List.of(
            Path.of("bagdir/data/random images/image01.png"),
            Path.of("bagdir/data/random images/image02.png"),
            Path.of("bagdir/data/random images/image03.png")
        );
        Mockito.doReturn(files).when(fileService).getAllFiles(Mockito.any());
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertDoesNotThrow(() -> checker.filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFiles().validate(Path.of("bagdir")));
    }

    @Test
    void filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFilesWithErrors() throws Exception {

        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image03.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";

        var document = parseXmlString(xml);

        var files = List.of(
            Path.of("data/random images/image01.png"),
            Path.of("data/random images/image02.png"),
            Path.of("data/random images/image04.png")
        );

        Mockito.doReturn(files).when(fileService).getAllFiles(Mockito.any());
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        var e = assertThrows(RuleViolationDetailsException.class,
            () -> checker.filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFiles().validate(Path.of("bagdir")));

        assertTrue(e.getLocalizedMessage().contains("image03.png"));
        assertTrue(e.getLocalizedMessage().contains("image04.png"));
    }

    @Test
    void filesXmlAllFilesHaveFormat() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.jpeg\">\n"
            + "        <dcterms:format>image/jpeg</dcterms:format>\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image03.jpeg\">\n"
            + "        <dcterms:format>image/jpeg</dcterms:format>\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/a/deeper/path/With some file.txt\">\n"
            + "        <dcterms:format>text/plain</dcterms:format>\n"
            + "        <dcterms:created>2016-11-09</dcterms:created>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";
        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertDoesNotThrow(() -> checker.filesXmlAllFilesHaveFormat().validate(Path.of("bagdir")));
    }

    @Test
    void filesXmlAllFilesHaveFormatButSomeDont() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.jpeg\">\n"
            + "        <dcterms:format>image/jpeg</dcterms:format>\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image03.jpeg\">\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/a/deeper/path/With some file.txt\">\n"
            + "        <dcterms:format>text/plain</dcterms:format>\n"
            + "        <dcterms:created>2016-11-09</dcterms:created>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";
        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertThrows(RuleViolationDetailsException.class,
            () -> checker.filesXmlAllFilesHaveFormat().validate(Path.of("bagdir")));
    }

    @Test
    void filesXmlFilesHaveOnlyAllowedNamespaces() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:something=\"http://dans.knaw.nl/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.jpeg\">\n"
            + "        <dcterms:format>image/jpeg</dcterms:format>\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image03.jpeg\">\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/a/deeper/path/With some file.txt\">\n"
            + "        <dcterms:format>text/plain</dcterms:format>\n"
            + "        <dcterms:created>2016-11-09</dcterms:created>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";
        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertDoesNotThrow(() -> checker.filesXmlFilesHaveOnlyAllowedNamespaces().validate(Path.of("bagdir")));
    }

    @Test
    void filesXmlFilesHaveOnlyAllowedNamespacesButOneIsDifferent() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:something=\"http://dans.knaw.nl/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.jpeg\">\n"
            + "        <something:format>image/jpeg</something:format>\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image03.jpeg\">\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/a/deeper/path/With some file.txt\">\n"
            + "        <dcterms:format>text/plain</dcterms:format>\n"
            + "        <dcterms:created>2016-11-09</dcterms:created>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";
        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertThrows(RuleViolationDetailsException.class,
            () -> checker.filesXmlFilesHaveOnlyAllowedNamespaces().validate(Path.of("bagdir")));
    }

    @Test
    void filesXmlFilesHaveOnlyAllowedAccessRights() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.jpeg\">\n"
            + "        <dcterms:format>image/jpeg</dcterms:format>\n"
            + "        <dcterms:accessRights>ANONYMOUS</dcterms:accessRights>\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image03.jpeg\">\n"
            + "        <dcterms:format>image/jpeg</dcterms:format>\n"
            + "        <dcterms:accessRights>RESTRICTED_REQUEST</dcterms:accessRights>\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image04.jpeg\">\n"
            + "        <dcterms:format>image/jpeg</dcterms:format>\n"
            + "        <dcterms:accessRights>NONE</dcterms:accessRights>\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";
        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        assertDoesNotThrow(() -> checker.filesXmlFilesHaveOnlyAllowedAccessRights().validate(Path.of("bagdir")));
    }

    @Test
    void filesXmlFilesHaveOnlyAllowedAccessRightsButOneIsIncorrected() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "        <dcterms:description>This description will be archived, but not displayed anywhere in the Web-UI</dcterms:description>\n"
            + "        <dcterms:format>image/png</dcterms:format>\n"
            + "        <dcterms:created>2016-11-11</dcterms:created>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.jpeg\">\n"
            + "        <dcterms:format>image/jpeg</dcterms:format>\n"
            + "        <dcterms:accessRights>WRONG_VALUE</dcterms:accessRights>\n"
            + "        <dcterms:created>2016-11-10</dcterms:created>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";
        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var checker = new FilesXmlValidatorImpl(xmlReader, fileService, originalFilepathsService);

        var e = assertThrows(RuleViolationDetailsException.class,
            () -> checker.filesXmlFilesHaveOnlyAllowedAccessRights().validate(Path.of("bagdir")));

        assertTrue(e.getExceptions().get(0).getLocalizedMessage().contains("WRONG_VALUE"));
    }
}