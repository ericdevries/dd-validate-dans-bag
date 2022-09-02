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

import nl.knaw.dans.validatedansbag.core.engine.RuleViolationDetailsException;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsService;
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FilesXmlRulesImpl implements FilesXmlRules {

    private static final Logger log = LoggerFactory.getLogger(FilesXmlRulesImpl.class);

    private final XmlReader xmlReader;

    private final FileService fileService;

    private final OriginalFilepathsService originalFilepathsService;

    public FilesXmlRulesImpl(XmlReader xmlReader, FileService fileService, OriginalFilepathsService originalFilepathsService) {
        this.xmlReader = xmlReader;
        this.fileService = fileService;
        this.originalFilepathsService = originalFilepathsService;
    }

    @Override
    public BagValidatorRule filesXmlFilePathAttributesContainLocalBagPathAndNonPayloadFilesAreNotDescribed() {
        return path -> {
            var errors = new ArrayList<RuleViolationDetailsException>();

            // Each file element's filepath attribute MUST contain the bag local path to the payload file described.
            try {
                filesXmlFileElementsAllHaveFilepathAttribute(path);
            }
            catch (RuleViolationDetailsException e) {
                errors.add(e);
            }

            // 2.6.2 is already checked
            // Directories and non-payload files MUST NOT be described by a file element.
            try {
                filesXmlDescribesOnlyPayloadFiles(path);
            }
            catch (RuleViolationDetailsException e) {
                errors.add(e);
            }

            if (errors.size() > 0) {
                throw new RuleViolationDetailsException(errors);
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlNoDuplicateFilesAndEveryPayloadFileIsDescribed() {
        return path -> {
            var errors = new ArrayList<RuleViolationDetailsException>();

            // There MUST NOT be more than one file element corresponding to a payload file
            try {
                filesXmlNoDuplicates(path);
            }
            catch (RuleViolationDetailsException e) {
                errors.add(e);
            }

            // every payload file MUST be described by a file element.
            try {
                filesXmlDescribesAllPayloadFiles(path);
            }
            catch (RuleViolationDetailsException e) {
                errors.add(e);
            }

            if (errors.size() > 0) {
                throw new RuleViolationDetailsException(errors);
            }
        };
    }

    void filesXmlDescribesAllPayloadFiles(Path path) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, RuleViolationDetailsException {
        var dataPath = path.resolve("data");
        var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));

        var searchExpressions = List.of(
            "/files:files/files:file/@filepath",
            "/files/file/@filepath");

        var filePathNodes = xmlReader.xpathsToStream(document, searchExpressions).collect(Collectors.toList());

        // find all files that exist on disk
        var bagPaths = fileService.getAllFiles(dataPath)
            .stream()
            .map(path::relativize)
            .collect(Collectors.toSet());

        var bagPathMapping = originalFilepathsService.getMappingsFromOriginalToRenamed(path);

        var xmlPaths = filePathNodes.stream()
            .map(Node::getTextContent)
            .map(Path::of)
            .map(Path::normalize)
            .map(p -> Optional.ofNullable(bagPathMapping.get(p)).orElse(p))
            .collect(Collectors.toSet());

        var onlyInBag = CollectionUtils.subtract(bagPaths, xmlPaths);

        if (onlyInBag.size() > 0) {
            var msg = onlyInBag.stream()
                .map(Path::toString)
                .collect(Collectors.joining(", "));

            throw new RuleViolationDetailsException(String.format(
                "files.xml does not describe all payload files: {%s}", msg));

        }
    }

    void filesXmlDescribesOnlyPayloadFiles(Path path) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, RuleViolationDetailsException {
        var dataPath = path.resolve("data");
        var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));

        // elements may be namespaced, or not, so find both versions
        var searchExpressions = List.of(
            "/files:files/files:file/@filepath",
            "/files/file/@filepath");

        var filePathNodes = xmlReader.xpathsToStream(document, searchExpressions).collect(Collectors.toList());

        // find all files that exist on disk
        var bagPaths = fileService.getAllFiles(dataPath)
            .stream()
            .map(path::relativize)
            .collect(Collectors.toSet());

        var bagPathMapping = originalFilepathsService.getMappingsFromOriginalToRenamed(path);

        // transform paths in xml to Path objects
        var xmlPaths = filePathNodes.stream()
            .map(Node::getTextContent)
            .map(Path::of)
            .map(Path::normalize)
            .map(p -> Optional.ofNullable(bagPathMapping.get(p)).orElse(p))
            .collect(Collectors.toSet());

        // compare the 2 sets. If elements exist in files.xml that are not in the bag dir
        // throw an exception
        var onlyInXml = CollectionUtils.subtract(xmlPaths, bagPaths);

        if (onlyInXml.size() > 0) {
            var incorrectFiles = onlyInXml.stream()
                .map(Path::toString)
                .collect(Collectors.joining(", "));

            throw new RuleViolationDetailsException(String.format(
                "files.xml describes non-payload files or directories: {%s}", incorrectFiles
            ));
        }
    }

    void filesXmlFileElementsAllHaveFilepathAttribute(Path path)
        throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, RuleViolationDetailsException {
        var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));

        var missingAttributes = xmlReader.xpathToStream(document, "/files/file")
            .filter(node -> {
                var attributes = node.getAttributes();
                var attr = attributes.getNamedItem("filepath");

                return attr == null || attr.getTextContent().isEmpty();
            })
            .collect(Collectors.toList());

        if (!missingAttributes.isEmpty()) {
            throw new RuleViolationDetailsException(String.format(
                "%s 'file' element(s) don't have a 'filepath' attribute", missingAttributes.size()
            ));
        }
    }

    void filesXmlNoDuplicates(Path path) throws RuleViolationDetailsException, IOException, XPathExpressionException, ParserConfigurationException, SAXException {
        var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));

        var searchExpressions = List.of(
            "/files:files/files:file/@filepath",
            "/files/file/@filepath");

        var filePathNodes = xmlReader.xpathsToStream(document, searchExpressions).collect(Collectors.toList());

        // list all duplicate entries in files.xml
        var duplicatePaths = filePathNodes.stream()
            .map(Node::getTextContent)
            .map(Path::of)
            .collect(Collectors.groupingBy(Path::normalize))
            .entrySet()
            .stream()
            .filter(item -> item.getValue().size() > 1)
            .collect(Collectors.toSet());

        if (duplicatePaths.size() > 0) {
            var msg = duplicatePaths.stream().map(Map.Entry::getKey).map(Path::toString).collect(Collectors.joining(", "));

            throw new RuleViolationDetailsException(String.format(
                "files.xml duplicate entries found: {%s}", msg
            ));
        }
    }
}
