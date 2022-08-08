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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BagInfoCheckerImpl implements BagInfoChecker {
    private static final Logger log = LoggerFactory.getLogger(BagInfoCheckerImpl.class);

    private final FileService fileService;

    private final BagItMetadataReader bagItMetadataReader;
    private final BagXmlReader bagXmlReader;

    private final Pattern doiPattern = Pattern.compile("^10(\\.\\d+)+/.+");

    private final Pattern doiUrlPattern = Pattern.compile("^((https?://(dx\\.)?)?doi\\.org/(urn:)?(doi:)?)?10(\\.\\d+)+/.+");
    private final Pattern urnPattern = Pattern.compile("^urn:[A-Za-z0-9][A-Za-z0-9-]{0,31}:[a-z0-9()+,\\-\\\\.:=@;$_!*'%/?#]+$");

    private final String daiPrefix = "info:eu-repo/dai/nl/";

    private final DaiDigestCalculator daiDigestCalculator;

    private final PolygonListValidator polygonListValidator;

    public BagInfoCheckerImpl(FileService fileService, BagItMetadataReader bagItMetadataReader, BagXmlReader bagXmlReader, DaiDigestCalculator daiDigestCalculator,
        PolygonListValidator polygonListValidator) {
        this.fileService = fileService;
        this.bagItMetadataReader = bagItMetadataReader;
        this.bagXmlReader = bagXmlReader;
        this.daiDigestCalculator = daiDigestCalculator;
        this.polygonListValidator = polygonListValidator;
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
                throw new RuleViolationDetailsException("Unexpected exception occurred while processing", e);
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
                    .map(Node::getTextContent)
                    .collect(Collectors.joining(", "));

                if (match.length() > 0) {
                    throw new RuleViolationDetailsException("Invalid DOIs: " + match);
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected exception occurred while processing", e);
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
                throw new RuleViolationDetailsException("Unexpected exception occurred while processing", e);
            }

        };
    }

    @Override
    public BagValidatorRule ddmGmlPolygonPosListIsWellFormed() {
        return (path) -> {

            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
                var expr = "//dcx-gml:spatial//*[local-name() = 'posList']";
                var nodes = (NodeList) bagXmlReader.evaluateXpath(document, expr, XPathConstants.NODESET);

                var match = IntStream.range(0, nodes.getLength())
                    .mapToObj(nodes::item)
                    .map(Node::getTextContent)
                    .map((posList) -> {

                        try {
                            polygonListValidator.validatePolygonList(posList);
                        }
                        catch (PolygonListValidator.PolygonValidationException e) {
                            return new RuleViolationDetailsException(e.getLocalizedMessage(), e);
                        }

                        return null;
                    })
                    .filter(Objects::nonNull)
                    .map(Throwable::getLocalizedMessage)
                    .collect(Collectors.toList());

                if (!match.isEmpty()) {
                    var message = String.join("\n", match);
                    throw new RuleViolationDetailsException("Invalid posList: " + message);
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected exception occurred while processing", e);
            }
        };
    }

    @Override
    public BagValidatorRule polygonsInSameMultiSurfaceHaveSameSrsName() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
                var expr = "//*[local-name() = 'MultiSurface']";
                var nodes = (NodeList) bagXmlReader.evaluateXpath(document, expr, XPathConstants.NODESET);

                var match = IntStream.range(0, nodes.getLength())
                    .mapToObj(nodes::item)
                    .filter(node -> {

                        try {
                            var poly = (NodeList) bagXmlReader.evaluateXpath(node, ".//*[local-name() = 'Polygon']", XPathConstants.NODESET);

                            var srsNames = IntStream.range(0, poly.getLength())
                                .mapToObj(poly::item)
                                .map(p -> p.getAttributes().getNamedItem("srsName"))
                                .filter(Objects::nonNull)
                                .map(Node::getTextContent)
                                .collect(Collectors.toSet());

                            if (srsNames.size() > 1) {
                                return true;
                            }
                        }
                        catch (Throwable e) {
                            log.error("Error checking srsNames attribute", e);
                            return true;
                        }

                        return false;
                    })
                    .collect(Collectors.toList());

                if (!match.isEmpty()) {
                    throw new RuleViolationDetailsException("Found MultiSurface element containing polygons with different srsNames");
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected exception occurred while processing", e);
            }
        };
    }

    @Override
    public BagValidatorRule pointsHaveAtLeastTwoValues() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

                // points
                var expr = "//*[local-name() = 'Point' or local-name() = 'lowerCorner' or local-name() = 'upperCorner']";
                var nodes = (NodeList) bagXmlReader.evaluateXpath(document, expr, XPathConstants.NODESET);

                var match = IntStream.range(0, nodes.getLength())
                    .mapToObj(nodes::item)
                    .filter(node -> node.getNamespaceURI().equals("http://www.opengis.net/gml"))
                    //.map(Node::getTextContent)
                    //.map(String::trim)
                    .collect(Collectors.toList());

                var errors = new ArrayList<RuleViolationDetailsException>();

                for (var value : match) {
                    var attr = value.getParentNode().getAttributes().getNamedItem("srsName");
                    var text = value.getTextContent();
                    var isRD = attr != null && "urn:ogc:def:crs:EPSG::28992".equals(attr.getTextContent());

                    try {
                        var parts = Arrays.stream(text.split("\\s+"))
                            .map(Float::parseFloat)
                            .collect(Collectors.toList());

                        if (parts.size() < 2) {
                            errors.add(new RuleViolationDetailsException(String.format(
                                "Point has less than two coordinates: %s", text
                            )));
                        }

                        else if (isRD) {
                            var x = parts.get(0);
                            var y = parts.get(1);

                            var valid = x >= -7000 && x <= 300000 && y >= 289000 && y <= 629000;

                            if (!valid) {
                                errors.add(new RuleViolationDetailsException(String.format(
                                    "Point is outside RD bounds: %s", text
                                )));
                            }
                        }
                    }
                    catch (NumberFormatException e) {
                        errors.add(new RuleViolationDetailsException(String.format(
                            "Point has non numeric coordinates: %s", text
                        )));
                    }
                }

                if (errors.size() > 0) {
                    throw new RuleViolationDetailsException(errors);
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Error reading XML file", e);
            }
        };
    }

    @Override
    public BagValidatorRule archisIdentifiersHaveAtMost10Characters() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

                // points
                var expr = "//dcterms:identifier[@xsi:type = 'id-type:ARCHIS-ZAAK-IDENTIFICATIE']";
                var nodes = (NodeList) bagXmlReader.evaluateXpath(document, expr, XPathConstants.NODESET);

                var match = IntStream.range(0, nodes.getLength())
                    .mapToObj(nodes::item)
                    .map(Node::getTextContent)
                    .filter(Objects::nonNull)
                    .filter(text -> text.length() > 10)
                    .collect(Collectors.toList());

                if (match.size() > 0) {
                    var exceptions = match.stream().map(e -> new RuleViolationDetailsException(String.format(
                        "Archis identifier must be 10 or fewer characters long: %s", e
                    ))).collect(Collectors.toList());

                    throw new RuleViolationDetailsException(exceptions);
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected exception occurred while processing", e);
            }
        };
    }

    Stream<Node> xpathToStream(Node node, String expression) throws XPathExpressionException {
        var nodes = (NodeList) bagXmlReader.evaluateXpath(node, expression, XPathConstants.NODESET);

        return IntStream.range(0, nodes.getLength())
            .mapToObj(nodes::item);
    }

    @Override
    public BagValidatorRule allUrlsAreValid() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

                var hrefNodes = xpathToStream(document, "*/@href");
                var schemeURINodes = xpathToStream(document, "//ddm:subject/@schemeURI");
                var valueURINodes = xpathToStream(document, "//ddm:subject/@valueURI");

                var elementSelectors = Stream.of(
                        "//*[@xsi:type='dcterms:URI']",
                        "//*[@xsi:type='dcterms:URL']",
                        "//*[@xsi:type='URI']",
                        "//*[@xsi:type='URL']",
                        "//*[@scheme='dcterms:URI']",
                        "//*[@scheme='dcterms:URL']",
                        "//*[@scheme='URI']",
                        "//*[@scheme='URL']"
                    ).map(selector -> {
                        try {
                            return xpathToStream(document, selector);
                        }
                        catch (XPathExpressionException e) {
                            log.error("Unable to parse document", e);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .flatMap(i -> i);

                var doiValues = Stream.of(
                        "//*[@scheme = 'id-type:DOI']",
                        "//*[@scheme = 'DOI']"
                    ).map(selector -> {
                        try {
                            return xpathToStream(document, selector);
                        }
                        catch (XPathExpressionException e) {
                            log.error("Unable to parse document", e);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .flatMap(i -> i)
                    .filter(node -> node.getAttributes().getNamedItem("href") == null);

                var urnValues = Stream.of(
                        "//*[@scheme = 'id-type:URN']",
                        "//*[@scheme = 'URN']"
                    ).map(selector -> {
                        try {
                            return xpathToStream(document, selector);
                        }
                        catch (XPathExpressionException e) {
                            log.error("Unable to parse document", e);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .flatMap(i -> i)
                    .filter(node -> node.getAttributes().getNamedItem("href") == null);

                var nodes = Stream.of(hrefNodes, schemeURINodes, valueURINodes, elementSelectors)
                    .flatMap(i -> i)
                    .map(node -> {
                        var value = node.getTextContent();

                        try {
                            var uri = new URI(value);

                            if (!List.of("http", "https").contains(uri.getScheme())) {
                                return new RuleViolationDetailsException(String.format(
                                    "protocol '%s' in uri '%s' is not one of the accepted protocols [http, https]", uri.getScheme(), uri
                                ));
                            }
                        }
                        catch (URISyntaxException e) {
                            return new RuleViolationDetailsException(String.format(
                                "'%s' is not a valid uri", value
                            ));
                        }

                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                var dois = doiValues
                    .map(Node::getTextContent)
                    .filter(textContent -> !doiUrlPattern.matcher(textContent).matches())
                    .collect(Collectors.joining(", "));

                var urns = urnValues
                    .map(Node::getTextContent)
                    .filter(textContent -> !urnPattern.matcher(textContent).matches())
                    .collect(Collectors.joining(", "));

                var errors = new ArrayList<>(nodes);

                if (dois.length() > 0) {
                    errors.add(new RuleViolationDetailsException(String.format("Invalid DOIs: %s", dois)));
                }

                if (urns.length() > 0) {
                    errors.add(new RuleViolationDetailsException(String.format("Invalid URNs: %s", urns)));
                }

                if (errors.size() > 0) {
                    throw new RuleViolationDetailsException(errors);
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected exception occurred while processing", e);
            }
        };
    }

    @Override
    public BagValidatorRule ddmMustHaveRightsHolder() {
        return (path) -> {

            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

                // copied from easy-validate-dans-bag
                // TODO FIXME: this will true if there is a <role>rightsholder</role> anywhere in the document
                var inRole = xpathToStream(document, "//*[local-name() = 'role']")
                    .filter(node -> node.getTextContent().contains("rightsholder"))
                    .findFirst();

                var rightsHolder = xpathToStream(document, "//ddm:dcmiMetadata//dcterms:rightsHolder")
                    .map(Node::getTextContent)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .findFirst();

                if (inRole.isEmpty() && rightsHolder.isEmpty()) {
                    throw new RuleViolationDetailsException("No rightsholder");
                }
            }

            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected exception occurred while processing", e);
            }
        };
    }

    @Override
    public BagValidatorRule xmlFileConfirmsToSchema(Path file, String schema) {
        return (path) -> {

            try {
                var document = bagXmlReader.readXmlFile(path.resolve(file));
                var results = bagXmlReader.validateXmlWithSchema(document, "ddm");

                if (results.size() > 0) {
                    // TODO see how we can get all the errors in there
                    throw new RuleViolationDetailsException(String.format(
                        "%s does not confirm to %s", file, schema
                    ));
                }
            } catch (Exception e) {
                throw new RuleViolationDetailsException(String.format(
                    "%s does not confirm to %s", file, schema
                ), e);
            }
        };
    }
}
