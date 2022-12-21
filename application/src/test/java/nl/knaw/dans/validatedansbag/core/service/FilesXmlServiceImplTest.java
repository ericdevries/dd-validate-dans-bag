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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilesXmlServiceImplTest {

    final XmlReader xmlReader = Mockito.spy(new XmlReaderImpl());

    private Document parseXmlString(String str) throws ParserConfigurationException, IOException, SAXException {
        return new XmlReaderImpl().readXmlString(str);
    }

    @Test
    void readFilepaths() throws Exception {
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
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var filesXmlService = new FilesXmlServiceImpl(xmlReader);
        var result = filesXmlService.readFilepaths(Path.of("test")).collect(Collectors.toSet());

        var expected = Set.of(
            Path.of("data/random images/image01.png"),
            Path.of("data/random images/image02.png"),
            Path.of("data/random images/image03.png")
        );

        assertEquals(expected, result);
    }

    @Test
    void readFilepathsWithoutNamespace() throws Exception {
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
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var filesXmlService = new FilesXmlServiceImpl(xmlReader);
        var result = filesXmlService.readFilepaths(Path.of("test")).collect(Collectors.toSet());

        var expected = Set.of(
            Path.of("data/random images/image01.png"),
            Path.of("data/random images/image02.png"),
            Path.of("data/random images/image03.png")
        );

        assertEquals(expected, result);
    }

    @Test
    void readFilepathsWithMissingAttributes() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<files xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "    <file filepath=\"data/random images/image01.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file filepath=\"data/random images/image02.png\">\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "    <file>\n"
            + "        <dcterms:title>The first image</dcterms:title>\n"
            + "    </file>\n"
            + "</files>\n"
            + "\n";

        var document = parseXmlString(xml);
        Mockito.doReturn(document).when(xmlReader).readXmlFile(Mockito.any());

        var filesXmlService = new FilesXmlServiceImpl(xmlReader);
        var result = filesXmlService.readFilepaths(Path.of("test")).collect(Collectors.toSet());

        var expected = Set.of(
            Path.of("data/random images/image01.png"),
            Path.of("data/random images/image02.png")
        );

        assertEquals(expected, result);
    }
}