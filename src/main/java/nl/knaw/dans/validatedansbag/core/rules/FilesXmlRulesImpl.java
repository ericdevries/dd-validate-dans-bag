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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        // 2.6.2 is already checked
        // Directories and non-payload files MUST NOT be described by a file element.
        return (path) -> {
            var missingInFilesXml = filesXmlDescribesOnlyPayloadFiles(path);

            if (missingInFilesXml.size() > 0) {
                var paths = missingInFilesXml.stream().map(Path::toString).collect(Collectors.joining(", "));
                return RuleResult.error(String.format("files.xml: duplicate entries found: {%s}", paths));
            }

            return RuleResult.ok();
        };
    }

    @Override
    public BagValidatorRule filesXmlNoDuplicateFilesAndEveryPayloadFileIsDescribed() {
        return path -> {
            var errors = new ArrayList<String>();

            var duplicates = filesXmlNoDuplicates(path);

            // There MUST NOT be more than one file element corresponding to a payload file
            if (duplicates.size() > 0) {
                var paths = duplicates.stream().map(Path::toString).collect(Collectors.joining(", "));
                errors.add(String.format("files.xml: duplicate entries found: {%s}", paths));
            }

            var missingPayloadFiles = filesXmlDescribesAllPayloadFiles(path);

            // every payload file MUST be described by a file element.
            if (missingPayloadFiles.size() > 0) {
                var paths = missingPayloadFiles.stream().map(Path::toString).collect(Collectors.joining(", "));
                errors.add(String.format("files.xml does not describe all payload files: {%s}", paths));
            }

            if (errors.size() > 0) {
                return RuleResult.error(errors);
            }

            return RuleResult.ok();
        };
    }

    Set<Path> filesXmlDescribesAllPayloadFiles(Path path) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
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

        log.trace("Paths that exist on path {}: {}", dataPath, bagPaths);

        var bagPathMapping = originalFilepathsService.getMappingsFromOriginalToRenamed(path);

        var xmlPaths = filePathNodes.stream()
            .map(Node::getTextContent)
            .map(Path::of)
            .map(Path::normalize)
            .map(p -> Optional.ofNullable(bagPathMapping.get(p)).orElse(p))
            .collect(Collectors.toSet());

        log.trace("Paths that defined in files.xml: {}", xmlPaths);

        var result = CollectionUtils.subtract(bagPaths, xmlPaths);

        log.debug("Difference between filesystem entries and files.xml content: {}", result);

        return new HashSet<>(result);
    }

    Set<Path> filesXmlDescribesOnlyPayloadFiles(Path path) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
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

        log.trace("Paths that exist on path {}: {}", dataPath, bagPaths);

        var bagPathMapping = originalFilepathsService.getMappingsFromOriginalToRenamed(path);

        // transform paths in xml to Path objects
        var xmlPaths = filePathNodes.stream()
            .map(Node::getTextContent)
            .map(Path::of)
            .map(Path::normalize)
            .map(p -> Optional.ofNullable(bagPathMapping.get(p)).orElse(p))
            .collect(Collectors.toSet());

        log.trace("Paths that defined in files.xml: {}", xmlPaths);
        // compare the 2 sets. If elements exist in files.xml that are not in the bag dir
        // throw an exception
        var onlyInXml = CollectionUtils.subtract(xmlPaths, bagPaths);

        log.debug("Difference between files.xml content and filesystem entries : {}", onlyInXml);

        return new HashSet<>(onlyInXml);
    }

    Set<Path> filesXmlNoDuplicates(Path path) throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {
        var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));

        var searchExpressions = List.of(
            "/files:files/files:file/@filepath",
            "/files/file/@filepath");

        var filePathNodes = xmlReader.xpathsToStream(document, searchExpressions).collect(Collectors.toList());

        // list all duplicate entries in files.xml

        return filePathNodes.stream()
            .map(Node::getTextContent)
            .map(Path::of)
            .collect(Collectors.groupingBy(Path::normalize))
            .entrySet()
            .stream()
            .filter(item -> item.getValue().size() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }
}
