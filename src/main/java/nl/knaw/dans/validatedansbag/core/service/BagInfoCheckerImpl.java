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

import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BagInfoCheckerImpl implements BagInfoChecker {
    private static final Logger log = LoggerFactory.getLogger(BagInfoCheckerImpl.class);

    private final FileService fileService;

    private final BagItMetadataReader bagItMetadataReader;
    private final BagXmlReader bagXmlReader;

    private final Pattern doiPattern = Pattern.compile("^10(\\.\\d+)+/.+");

    private final String daiPrefix = "info:eu-repo/dai/nl/";

    private final DaiDigestCalculator daiDigestCalculator;

    public BagInfoCheckerImpl(FileService fileService, BagItMetadataReader bagItMetadataReader, BagXmlReader bagXmlReader, DaiDigestCalculator daiDigestCalculator) {
        this.fileService = fileService;
        this.bagItMetadataReader = bagItMetadataReader;
        this.bagXmlReader = bagXmlReader;
        this.daiDigestCalculator = daiDigestCalculator;
    }

    @Override
    public BagValidatorRule bagIsValid() {
        return (path) -> {
            try {
                bagItMetadataReader.verifyBag(path);
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException(e.getLocalizedMessage(), e);
            }
        };
    }

    @Override
    public BagValidatorRule containsDir(Path dir) {
        return ((path) -> {
            var target = path.resolve(dir);

            if (!fileService.isDirectory(target)) {
                throw new RuleViolationDetailsException(
                    String.format("Path '%s' is not a directory", dir)
                );
            }
        });
    }

    @Override
    public BagValidatorRule containsFile(Path file) {
        return ((path) -> {
            var target = path.resolve(file);

            if (!fileService.isFile(target)) {
                throw new RuleViolationDetailsException(
                    String.format("Path '%s' is not a directory", file)
                );
            }
        });
    }

    @Override
    public BagValidatorRule bagInfoExistsAndIsWellFormed() {
        return path -> {
            if (!fileService.isFile(path.resolve(Path.of("bag-info.txt")))) {
                throw new RuleViolationDetailsException("bag-info.txt does not exist");
            }

            try {
                bagItMetadataReader.getBag(path).orElseThrow();
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("bag-info.txt exists but is malformed: " + e.getMessage(), e);
            }
        };
    }

    @Override
    public BagValidatorRule bagInfoCreatedElementIsIso8601Date() {
        return path -> {
            try {
                var created = bagItMetadataReader.getField(path, "Created").get(0);

                try {
                    DateTime.parse(created, ISODateTimeFormat.dateTime());
                }
                catch (Exception e) {
                    throw new RuleViolationDetailsException("Error", e);
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Error", e);
            }
        };
    }

    @Override
    public BagValidatorRule bagInfoContainsExactlyOneOf(String key) {
        return path -> {
            try {
                var items = bagItMetadataReader.getField(path, key);
                var amount = items == null ? 0 : items.size();

                if (amount != 1) {
                    throw new RuleViolationDetailsException(
                        String.format("bag-info.txt must contain exactly one '%s' element; number found: %s", key, amount)
                    );
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Error", e);
            }
        };
    }

    @Override
    public BagValidatorRule bagInfoContainsAtMostOneOf(String key) {
        return path -> {
            try {
                var items = bagItMetadataReader.getField(path, key);
                var amount = items == null ? 0 : items.size();

                if (amount > 1) {
                    throw new RuleViolationDetailsException(
                        String.format("bag-info.txt may contain at most one element: '%s'", key)
                    );
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Error", e);
            }
        };
    }

    RuleViolationDetailsException bagNotFoundException(Path path) {
        var message = String.format("Could not open bag on location '%s'", path);
        return new RuleViolationDetailsException(message);
    }

    @Override
    public BagValidatorRule bagShaPayloadManifestContainsAllPayloadFiles() {
        return (path) -> {
            var bag = bagItMetadataReader.getBag(path)
                .orElseThrow(() -> bagNotFoundException(path));

            try {
                var manifest = bagItMetadataReader.getBagManifest(bag, StandardSupportedAlgorithms.SHA1)
                    .orElseThrow(() -> new RuleViolationDetailsException("No manifest file found"));

                var filesInManifest = manifest.getFileToChecksumMap().keySet()
                    .stream().map(path::relativize).collect(Collectors.toSet());

                var filesInPayload = fileService.getAllFiles(path.resolve("data"))
                    .stream().map(path::relativize).collect(Collectors.toSet());

                if (!filesInManifest.equals(filesInPayload)) {
                    filesInPayload.removeAll(filesInManifest);

                    var filenames = filesInPayload.stream().map(Path::toString).collect(Collectors.joining(", "));
                    throw new RuleViolationDetailsException(String.format("All payload files must have an SHA-1 checksum. Files missing from SHA-1 manifest: %s", filenames));
                }
            }
            catch (RuleViolationDetailsException e) {
                throw e;
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected error occurred while validating manifest", e);
            }
        };
    }

    @Override
    public BagValidatorRule containsNothingElseThan(Path dir, String[] paths) {
        return (path) -> {
            try {
                var allowed = Arrays.stream(paths).map(p -> path.resolve(dir).resolve(p)).collect(Collectors.toSet());

                var allItems = fileService.getAllFilesAndDirectories(path.resolve(dir))
                    .stream().filter(p -> !allowed.contains(p))
                    // filter out the parent path
                    .filter(p -> !path.resolve(dir).equals(p))
                    .collect(Collectors.toSet());

                if (allItems.size() > 0) {
                    var filenames = allItems.stream().map(Path::toString).collect(Collectors.joining(", "));

                    throw new RuleViolationDetailsException(String.format(
                        "Directory %s contains files or directories that are not allowed: %s",
                        dir, filenames
                    ));
                }

            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected error occurred", e);
            }
        };
    }

    @Override
    public BagValidatorRule hasOnlyValidFileNames() {
        return (path) -> {
            var bag = bagItMetadataReader.getBag(path)
                .orElseThrow(() -> bagNotFoundException(path));

            var manifest = bagItMetadataReader.getBagManifest(bag, StandardSupportedAlgorithms.SHA1)
                .orElseThrow(() -> new RuleViolationDetailsException(String.format(
                    "Dependent rule should have failed: Could not get bag '%s'", path
                )));

            var invalidCharacters = ":*?\"<>|;#";

            var files = manifest.getFileToChecksumMap().keySet()
                .stream()
                .filter(file -> {
                    for (var c : invalidCharacters.toCharArray()) {
                        if (file.toString().indexOf(c) > -1) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(Path::toString)
                .collect(Collectors.joining(", "));

            if (files.length() > 0) {
                throw new RuleViolationDetailsException(String.format("Payload files must have valid characters. Invalid ones: %s", files));
            }
        };
    }

    @Override
    public BagValidatorRule optionalFileIsUtf8Decodable(Path filename) {
        return (path) -> {
            try {
                var target = path.resolve(filename);

                if (fileService.exists(target)) {
                    var contents = fileService.readFileContents(path.resolve(filename));
                    StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(contents));
                }
            }
            catch (CharacterCodingException e) {
                throw new RuleViolationDetailsException("Input not valid UTF-8: " + e.getMessage());
            }
            catch (IOException e) {
                throw new RuleViolationDetailsException("Exception when reading file: " + e.getMessage());
            }
        };
    }

    @Override
    public BagValidatorRule isOriginalFilepathsFileComplete() {
        return (path) -> {
            // TODO read files xml, apply original-filepaths.txt to it and do the magic
        };
    }

    @Override
    public BagValidatorRule ddmContainsUrnNbnIdentifier() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
                var expr = "//dcterms:identifier[@xsi:type=\"id-type:URN\"]";
                var nodes = (NodeList) bagXmlReader.evaluateXpath(document, expr, XPathConstants.NODESET);

                IntStream.range(0, nodes.getLength())
                    .mapToObj(nodes::item)
                    .filter((node) -> node.getTextContent().contains("urn:nbn"))
                    .findFirst()
                    .orElseThrow(() -> new RuleViolationDetailsException("URN:NBN identifier is missing"));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    @Override
    public BagValidatorRule ddmDoiIdentifiersAreValid() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
                var expr = "//dcterms:identifier[@xsi:type=\"id-type:DOI\"]";
                var nodes = (NodeList) bagXmlReader.evaluateXpath(document, expr, XPathConstants.NODESET);

                var match = IntStream.range(0, nodes.getLength())
                    .mapToObj(nodes::item)
                    .filter((node) -> {
                        var text = node.getTextContent();
                        return !doiPattern.matcher(text).matches();
                    })
                    .findFirst();

                if (match.isPresent()) {
                    throw new RuleViolationDetailsException("URN:NBN identifier is missing");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    @Override
    public BagValidatorRule ddmDaisAreValid() {
        return (path) -> {

            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
                var expr = "//dcx-dai:DAI";
                var nodes = (NodeList) bagXmlReader.evaluateXpath(document, expr, XPathConstants.NODESET);

                var match = IntStream.range(0, nodes.getLength())
                    .mapToObj(nodes::item)
                    .map(node -> node.getTextContent().replaceFirst(daiPrefix, ""))
                    .filter((id) -> {
                        var sum = daiDigestCalculator.calculateChecksum(id.substring(0, id.length() - 1), 9);
                        var last = id.charAt(id.length() - 1);

                        return sum != last;
                    })
                    .collect(Collectors.toList());

                if (!match.isEmpty()) {
                    var message = String.join(", ", match);
                    throw new RuleViolationDetailsException("Invalid DAIs: " + message);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        };
    }

    @Override
    public BagValidatorRule ddmGmlPolygonPosListIsWellFormed() {
        return (path) -> {

        };
    }

    @Override
    public BagValidatorRule polygonsInSameMultiSurfaceHaveSameSrsName() {
        return (path) -> {

        };
    }

    @Override
    public BagValidatorRule pointsHaveAtLeastTwoValues() {
        return (path) -> {

        };
    }

    @Override
    public BagValidatorRule archisIdentifiersHaveAtMost10Characters() {
        return (path) -> {

        };
    }

    @Override
    public BagValidatorRule allUrlsAreValid() {
        return (path) -> {

        };
    }

    @Override
    public BagValidatorRule ddmMustHaveRightsHolder() {
        return (path) -> {

        };
    }

    @Override
    public BagValidatorRule xmlFileConfirmsToSchema(Path file) {
        return (path) -> {

        };
    }
}
