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

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FilesXmlServiceImpl implements FilesXmlService {
    private final XmlReader xmlReader;

    public FilesXmlServiceImpl(XmlReader xmlReader) {
        this.xmlReader = xmlReader;
    }

    @Override
    public Stream<Path> readFilepaths(Path path) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));

        // elements may be namespaced, or not, so find both versions
        var searchExpressions = List.of(
            "/files:files/files:file/@filepath",
            "/files/file/@filepath");

        return xmlReader.xpathsToStreamOfStrings(document, searchExpressions)
            .map(Path::of);
    }
}
