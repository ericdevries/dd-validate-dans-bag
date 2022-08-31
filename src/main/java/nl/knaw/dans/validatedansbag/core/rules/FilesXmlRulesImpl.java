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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FilesXmlRulesImpl implements FilesXmlRules {

    private static final Logger log = LoggerFactory.getLogger(FilesXmlRulesImpl.class);

    private final XmlReader xmlReader;

    private final FileService fileService;

    private final OriginalFilepathsService originalFilepathsService;

    private final Set<String> allowedFilesXmlNamespaces = Set.of(
        XmlReader.NAMESPACE_DC,
        XmlReader.NAMESPACE_DCTERMS
    );

    private final Set<String> allowedAccessRights = Set.of("ANONYMOUS", "RESTRICTED_REQUEST", "NONE");

    public FilesXmlRulesImpl(XmlReader xmlReader, FileService fileService, OriginalFilepathsService originalFilepathsService) {
        this.xmlReader = xmlReader;
        this.fileService = fileService;
        this.originalFilepathsService = originalFilepathsService;
    }

    @Override
    public BagValidatorRule filesXmlHasDocumentElementFiles() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));
            var rootNode = document.getDocumentElement();

            if (rootNode == null || !"files".equals(rootNode.getNodeName())) {
                throw new RuleViolationDetailsException("files.xml document element must be 'files'");
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlHasOnlyFiles() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));
            var namespace = document.getNamespaceURI();

            if (XmlReader.NAMESPACE_FILES_XML.equals(namespace)) {
                log.debug("Rule filesXmlHasOnlyFiles has been checked by files.xsd");
            }
            else {
                var nonFiles = xmlReader.xpathToStream(document, "/files/*[local-name() != 'file']")
                    .collect(Collectors.toList());

                if (!nonFiles.isEmpty()) {
                    var nodeNames = nonFiles.stream().map(Node::getNodeName).collect(Collectors.joining(", "));

                    throw new RuleViolationDetailsException(String.format(
                        "Files.xml children of document element must only be 'file'. Found non-file elements: %s", nodeNames
                    ));
                }
            }

            var rootNode = document.getDocumentElement();

            if (rootNode == null || !"files".equals(rootNode.getNodeName())) {
                throw new RuleViolationDetailsException("files.xml document element must be 'files'");
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlFileElementsAllHaveFilepathAttribute() {
        return (path) -> {
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
        };
    }

    @Override
    public BagValidatorRule filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFiles() {
        return (path) -> {
            var dataPath = path.resolve("data");
            var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));

            if (originalFilepathsService.exists(path)) {
                log.debug("original-filepaths.txt exists, so checking is not needed");
                return;
            }

            var searchExpressions = List.of(
                "/files:files/files:file/@filepath",
                "/files/file/@filepath");

            var filePathNodes = xmlReader.xpathsToStream(document, searchExpressions).collect(Collectors.toList());

            var duplicatePaths = filePathNodes.stream()
                .map(Node::getTextContent)
                .map(Path::of)
                .collect(Collectors.groupingBy(Path::normalize))
                .entrySet()
                .stream()
                .filter(item -> item.getValue().size() > 1)
                .collect(Collectors.toSet());

            var bagPaths = fileService.getAllFiles(dataPath)
                .stream()
                .map(path::relativize)
                .collect(Collectors.toSet());

            var xmlPaths = filePathNodes.stream()
                .map(Node::getTextContent)
                .map(Path::of)
                .map(Path::normalize)
                .collect(Collectors.toSet());

            var onlyInBag = CollectionUtils.subtract(bagPaths, xmlPaths);
            var onlyInXml = CollectionUtils.subtract(xmlPaths, bagPaths);

            var message = new StringBuilder();

            if (duplicatePaths.size() > 0 || onlyInBag.size() > 0 || onlyInXml.size() > 0) {

                if (duplicatePaths.size() > 0) {
                    message.append("  - Duplicate filepaths found: ");
                    message.append(duplicatePaths.stream().map(Map.Entry::getKey).map(Path::toString).collect(Collectors.joining(", ")));
                    message.append("\n");
                }

                if (onlyInBag.size() > 0 || onlyInXml.size() > 0) {
                    message.append("  - Filepaths in files.xml not equal to files found in data folder. Difference - ");
                    message.append("only in bag: {");
                    message.append(onlyInBag.stream().map(Path::toString).collect(Collectors.joining(", ")));
                    message.append("} only in files.xml: {");
                    message.append(onlyInXml.stream().map(Path::toString).collect(Collectors.joining(", ")));
                    message.append("}");
                }

                throw new RuleViolationDetailsException(String.format(
                    "files.xml errors in filepath-attributes: \n%s", message
                ));
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlAllFilesHaveFormat() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));
            var expr = "//file";

            var wrongNodes = xmlReader.xpathToStream(document, expr).filter(node -> {
                try {
                    var size = xmlReader.xpathToStream(node, ".//dcterms:format").collect(Collectors.toSet()).size();

                    if (size == 0) {
                        return true;
                    }
                }
                catch (Exception e) {
                    log.error("Error running xpath expression", e);
                    return true;
                }

                return false;
            }).collect(Collectors.toList());

            if (wrongNodes.size() > 0) {
                throw new RuleViolationDetailsException("files.xml not all <file> elements contain a <dcterms:format>");
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlFilesHaveOnlyAllowedNamespaces() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));

            if (XmlReader.NAMESPACE_FILES_XML.equals(document.getNamespaceURI())) {
                log.debug("Rule filesXmlFilesHaveOnlyAllowedNamespaces has been checked by files.xsd");
            }

            var errors = xmlReader.xpathToStream(document, "//file/*")
                .filter(node -> !allowedFilesXmlNamespaces.contains(node.getNamespaceURI()))
                .collect(Collectors.toList());

            if (errors.size() > 0) {
                throw new RuleViolationDetailsException("files.xml: non-dc/dcterms elements found in some file elements");
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlFilesHaveOnlyAllowedAccessRights() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));

            var invalidNodes = xmlReader.xpathToStream(document, "//file/dcterms:accessRights")
                .filter(node -> !allowedAccessRights.contains(node.getTextContent()))
                .map(node -> {
                    var filePath = node.getParentNode().getAttributes().getNamedItem("filepath").getTextContent();

                    return new RuleViolationDetailsException(String.format(
                        "files.xml: invalid access rights %s in accessRights element for file: '%s'; allowed values %s",
                        node.getTextContent(), filePath, allowedAccessRights
                    ));
                })
                .collect(Collectors.toList());

            if (invalidNodes.size() > 0) {
                throw new RuleViolationDetailsException(invalidNodes);
            }
        };
    }
}
