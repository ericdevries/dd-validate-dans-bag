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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.FilesXmlService;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsService;
import org.apache.commons.collections4.CollectionUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class FilesXmlFilePathAttributesContainLocalBagPathAndNonPayloadFilesAreNotDescribed implements BagValidatorRule {
    private final FileService fileService;
    private final FilesXmlService filesXmlService;
    private final OriginalFilepathsService originalFilepathsService;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var missingInFilesXml = filesXmlDescribesOnlyPayloadFiles(path);

        if (missingInFilesXml.size() > 0) {
            var paths = missingInFilesXml.stream().map(Path::toString).collect(Collectors.joining(", "));
            return RuleResult.error(String.format("files.xml: duplicate entries found: {%s}", paths));
        }

        return RuleResult.ok();
    }

    Set<Path> filesXmlDescribesOnlyPayloadFiles(Path path) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        var dataPath = path.resolve("data");

        // find all files that exist on disk
        var bagPaths = fileService.getAllFiles(dataPath)
                .stream()
                .map(path::relativize)
                .collect(Collectors.toSet());

        log.trace("Paths that exist on path {}: {}", dataPath, bagPaths);

        var bagPathMapping = originalFilepathsService.getMappingsFromOriginalToRenamed(path);

        var xmlPaths = filesXmlService.readFilepaths(path)
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
}
