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

import gov.loc.repository.bagit.exceptions.CorruptChecksumException;
import gov.loc.repository.bagit.exceptions.FileNotInPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MissingBagitFileException;
import gov.loc.repository.bagit.exceptions.MissingPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.MissingPayloadManifestException;
import gov.loc.repository.bagit.exceptions.VerificationException;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import nl.knaw.dans.validatedansbag.core.engine.RuleSkipDependenciesException;
import nl.knaw.dans.validatedansbag.core.engine.RuleViolationDetailsException;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsService;
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidator;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidator;
import nl.knaw.dans.validatedansbag.core.validator.PolygonListValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.print.Doc;
import javax.xml.xpath.XPathExpressionException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BagRulesImpl implements BagRules {
    private static final Logger log = LoggerFactory.getLogger(BagRulesImpl.class);

    private final FileService fileService;

    private final BagItMetadataReader bagItMetadataReader;
    private final XmlReader xmlReader;

    private final OriginalFilepathsService originalFilepathsService;

    private final Pattern doiPattern = Pattern.compile("^10(\\.\\d+)+/.+");

    private final IdentifierValidator identifierValidator;

    private final PolygonListValidator polygonListValidator;

    private final LicenseValidator licenseValidator;

    public BagRulesImpl(FileService fileService, BagItMetadataReader bagItMetadataReader, XmlReader xmlReader, OriginalFilepathsService originalFilepathsService,
        IdentifierValidator identifierValidator,
        PolygonListValidator polygonListValidator, LicenseValidator licenseValidator) {
        this.fileService = fileService;
        this.bagItMetadataReader = bagItMetadataReader;
        this.xmlReader = xmlReader;
        this.originalFilepathsService = originalFilepathsService;
        this.identifierValidator = identifierValidator;
        this.polygonListValidator = polygonListValidator;
        this.licenseValidator = licenseValidator;
    }

    @Override
    public BagValidatorRule bagIsValid() {
        return (path) -> {
            try {
                bagItMetadataReader.verifyBag(path);
            }
            // only catch exceptions that have to do with the bag verification; other exceptions such as IOException should be propagated to the rule engine
            catch (InvalidBagitFileFormatException | MissingPayloadManifestException | MissingPayloadDirectoryException | FileNotInPayloadDirectoryException | MissingBagitFileException |
                   CorruptChecksumException | VerificationException e) {
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
                throw new RuleViolationDetailsException(String.format(
                    "bag-info.txt exists but is malformed: %s", e.getMessage()
                ), e);
            }
        };
    }

    @Override
    public BagValidatorRule bagInfoCreatedElementIsIso8601Date() {
        return path -> {
            var created = bagItMetadataReader.getSingleField(path, "Created");

            try {
                DateTime.parse(created, ISODateTimeFormat.dateTime());
            }
            catch (Throwable e) {
                throw new RuleViolationDetailsException(String.format(
                    "Date '%s' is not valid", created
                ), e);
            }
        };
    }

    @Override
    public BagValidatorRule bagInfoContainsExactlyOneOf(String key) {
        return path -> {
            var items = bagItMetadataReader.getField(path, key);

            if (items.size() != 1) {
                throw new RuleViolationDetailsException(
                    String.format("bag-info.txt must contain exactly one '%s' element; number found: %s", key, items.size())
                );
            }
        };
    }

    @Override
    public BagValidatorRule bagInfoContainsAtMostOneOf(String key) {
        return path -> {
            var items = bagItMetadataReader.getField(path, key);

            if (items.size() > 1) {
                throw new RuleViolationDetailsException(
                    String.format("bag-info.txt may contain at most one element: '%s'", key)
                );
            }
        };
    }

    @Override
    public BagValidatorRule bagInfoIsVersionOfIsValidUrnUuid() {
        return path -> {
            var items = bagItMetadataReader.getField(path, "Is-Version-Of");

            var invalidUrns = items.stream().filter(item -> {
                    try {
                        var uri = new URI(item);

                        if (!"urn".equalsIgnoreCase(uri.getScheme())) {
                            return true;
                        }

                        if (!uri.getSchemeSpecificPart().startsWith("uuid:")) {
                            return true;
                        }

                        //noinspection ResultOfMethodCallIgnored
                        UUID.fromString(uri.getSchemeSpecificPart().substring("uuid:".length()));
                    }
                    catch (URISyntaxException | IllegalArgumentException e) {
                        return true;
                    }

                    return false;
                })
                .collect(Collectors.toList());

            if (!invalidUrns.isEmpty()) {
                throw new RuleViolationDetailsException(
                    String.format("bag-info.txt Is-Version-Of value must be a valid URN: Invalid items {%s}", String.join(", ", invalidUrns))
                );
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
        };
    }

    @Override
    public BagValidatorRule containsNothingElseThan(Path dir, String[] paths) {
        return (path) -> {
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
        };
    }

    @Override
    public BagValidatorRule hasOnlyValidFileNames() {
        return (path) -> {
            var basePath = path.resolve("data");
            var invalidCharacters = ":*?\"<>|;#";

            var files = fileService.getAllFiles(basePath)
                .stream()
                .filter(f -> {
                    for (var c : invalidCharacters.toCharArray()) {
                        if (f.getFileName().toString().indexOf(c) > -1) {
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
                    fileService.readFileContents(target, StandardCharsets.UTF_8);
                }
                else {
                    throw new RuleSkipDependenciesException();
                }
            }
            catch (CharacterCodingException e) {
                throw new RuleViolationDetailsException("Input not valid UTF-8: " + e.getMessage());
            }
        };
    }

    @Override
    public BagValidatorRule originalFilePathsDoNotContainSpaces() {
        return (path) -> {
            var mapping = originalFilepathsService.getMapping(path);

            // the file is organized in a <physical-name><whitespace><original-name> fashion
            // so if there is whitespace in the physical name, it will be considered part of the original name
            // meaning this rule will never fail
            var invalidFilenames = mapping.stream()
                .map(p -> p.getRenamedFilename().toString())
                .filter(p -> p.matches(".*\\s+.*"))
                .collect(Collectors.toList());

            if (!invalidFilenames.isEmpty()) {
                var errors = String.join(", ", invalidFilenames);

                throw new RuleViolationDetailsException(String.format(
                    "original-filepaths.txt: physical relative path should not contain whitespace; culprits: %s", errors
                ));
            }
        };
    }

    @Override
    public BagValidatorRule isOriginalFilepathsFileComplete() {
        return (path) -> {
            if (!originalFilepathsService.exists(path)) {
                throw new RuleSkipDependenciesException();
            }

            var mapping = originalFilepathsService.getMapping(path);

            var document = xmlReader.readXmlFile(path.resolve("metadata/files.xml"));
            var searchExpressions = List.of(
                "/files:files/files:file/@filepath",
                "/files/file/@filepath");

            // the files defined in metadata/files.xml
            var fileXmlPaths = xmlReader.xpathsToStream(document, searchExpressions)
                .map(Node::getTextContent)
                .map(Path::of)
                .collect(Collectors.toSet());

            // the files on disk
            var actualFiles = fileService.getAllFiles(path.resolve("data"))
                .stream()
                .filter(i -> !path.resolve("data").equals(i))
                .map(path::relativize)
                .collect(Collectors.toSet());

            var renamedFiles = mapping.stream().map(OriginalFilepathsService.OriginalFilePathItem::getRenamedFilename).collect(Collectors.toSet());
            var originalFiles = mapping.stream().map(OriginalFilepathsService.OriginalFilePathItem::getOriginalFilename).collect(Collectors.toSet());

            // disjunction returns the difference between 2 sets
            // so {1,2,3} disjunction {2,3,4} would return {1,4}
            var physicalFileSetsDiffer = CollectionUtils.disjunction(actualFiles, renamedFiles).size() > 0;
            var originalFileSetsDiffer = CollectionUtils.disjunction(fileXmlPaths, originalFiles).size() > 0;

            if (physicalFileSetsDiffer || originalFileSetsDiffer) {
                //  items that exist only in actual files, but not in the keyset of mapping and not in the files.xml
                var onlyInBag = CollectionUtils.subtract(actualFiles, renamedFiles);

                // files that only exist in files.xml, but not in the original-filepaths.txt
                var onlyInFilesXml = CollectionUtils.subtract(fileXmlPaths, originalFiles);

                // files that only exist in original-filepaths.txt, but not on the disk
                var onlyInFilepathsPhysical = CollectionUtils.subtract(renamedFiles, actualFiles);

                // files that only exist in original-filepaths.txt, but not in files.xml
                var onlyInFilepathsOriginal = CollectionUtils.subtract(originalFiles, fileXmlPaths);

                var message = new StringBuilder();

                if (physicalFileSetsDiffer) {
                    message.append("  - Physical file paths in original-filepaths.txt not equal to payload in data dir. Difference - ");
                    message.append("only in payload: {")
                        .append(onlyInBag.stream().map(Path::toString).collect(Collectors.joining(", ")))
                        .append("}");
                    message.append(", only in physical-bag-relative-path: {")
                        .append(onlyInFilepathsPhysical.stream().map(Path::toString).collect(Collectors.joining(", ")))
                        .append("}");
                    message.append("\n");
                }

                if (originalFileSetsDiffer) {
                    message.append("  - Original file paths in original-filepaths.txt not equal to filepaths in files.xml. Difference - ");
                    message.append("only in files.xml: {")
                        .append(onlyInFilesXml.stream().map(Path::toString).collect(Collectors.joining(", ")))
                        .append("}");
                    message.append(", only in original-bag-relative-path: {")
                        .append(onlyInFilepathsOriginal.stream().map(Path::toString).collect(Collectors.joining(", ")))
                        .append("}");
                    message.append("\n");
                }

                throw new RuleViolationDetailsException(String.format(
                    "original-filepaths.txt errors: \n%s", message
                ));
            }
        };
    }

    @Override
    public BagValidatorRule ddmMayContainDctermsLicenseFromList() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
            var expr = "//ddm:dcmiMetadata/dcterms:license[@xsi:type]";

            var nodes = xmlReader.xpathToStream(document, expr).collect(Collectors.toList());

            if (nodes.size() == 0) {
                throw new RuleViolationDetailsException("No licenses found");
            }

            var node = nodes.get(0);
            var license = nodes.get(0).getTextContent();
            var attr = node.getAttributes().getNamedItem("xsi:type").getTextContent();

            // converts a namespace uri into a prefix that is used in the document
            var prefix = document.lookupPrefix(XmlReader.NAMESPACE_DCTERMS);

            if (!attr.equals(String.format("%s:URI", prefix))) {
                throw new RuleViolationDetailsException("No license with xsi:type=\"dcterms:URI\"");
            }

            if (!licenseValidator.isValidLicense(license)) {
                throw new RuleViolationDetailsException(String.format(
                    "Found unknown or unsupported license: %s", license
                ));
            }
        };
    }

    @Override
    public BagValidatorRule ddmDoiIdentifiersAreValid() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
            var expr = "//dcterms:identifier[@xsi:type=\"id-type:DOI\"]";

            var nodes = xmlReader.xpathToStream(document, expr);
            var match = nodes.filter((node) -> {
                    var text = node.getTextContent();
                    return !doiPattern.matcher(text).matches();
                })
                .map(Node::getTextContent)
                .collect(Collectors.joining(", "));

            if (match.length() > 0) {
                throw new RuleViolationDetailsException("Invalid DOIs: " + match);
            }
        };
    }

    @Override
    public BagValidatorRule ddmDaisAreValid() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
            var expr = "//dcx-dai:DAI";
            var match = xmlReader.xpathToStream(document, expr)
                .map(Node::getTextContent)
                .filter((id) -> !identifierValidator.validateDai(id))
                .collect(Collectors.toList());

            if (!match.isEmpty()) {
                var message = String.join(", ", match);
                throw new RuleViolationDetailsException("Invalid DAIs: " + message);
            }
        };
    }

    @Override
    public BagValidatorRule ddmIsnisAreValid() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
            var expr = "//dcx-dai:ISNI";
            var match = xmlReader.xpathToStream(document, expr)
                .map(Node::getTextContent)
                .filter((id) -> !identifierValidator.validateIsni(id))
                .collect(Collectors.toList());

            if (!match.isEmpty()) {
                var message = String.join(", ", match);
                throw new RuleViolationDetailsException("Invalid ISNI(s): " + message);
            }
        };
    }

    @Override
    public BagValidatorRule ddmOrcidsAreValid() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
            var expr = "//dcx-dai:ORCID";
            var match = xmlReader.xpathToStream(document, expr)
                .map(Node::getTextContent)
                .filter((id) -> !identifierValidator.validateOrcid(id))
                .collect(Collectors.toList());

            if (!match.isEmpty()) {
                var message = String.join(", ", match);
                throw new RuleViolationDetailsException("Invalid ORCID(s): " + message);
            }
        };
    }

    @Override
    public BagValidatorRule ddmGmlPolygonPosListIsWellFormed() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
            var expr = "//dcx-gml:spatial//*[local-name() = 'posList']";
            var nodes = xmlReader.xpathToStream(document, expr);

            var match = nodes.map(Node::getTextContent)
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
        };
    }

    @Override
    public BagValidatorRule polygonsInSameMultiSurfaceHaveSameSrsName() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
            var expr = "//gml:MultiSurface";
            var nodes = xmlReader.xpathToStream(document, expr);
            var match = nodes.filter(node -> {
                    try {
                        var srsNames = xmlReader.xpathToStream(node, ".//gml:Polygon")
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
        };
    }

    @Override
    public BagValidatorRule pointsHaveAtLeastTwoValues() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

            // points
            var expr = "//gml:Point | //gml:lowerCorner | //gml:upperCorner";
            var match = xmlReader.xpathToStream(document, expr).collect(Collectors.toList());

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
        };
    }

    @Override
    public BagValidatorRule archisIdentifiersHaveAtMost10Characters() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

            // points
            var expr = "//dcterms:identifier[@xsi:type = 'id-type:ARCHIS-ZAAK-IDENTIFICATIE']";
            var match = xmlReader.xpathToStream(document, expr)
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
        };
    }

    @Override
    public BagValidatorRule allUrlsAreValid() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

            var hrefNodes = xmlReader.xpathToStream(document, "*/@href");
            var schemeURINodes = xmlReader.xpathToStream(document, "//ddm:subject/@schemeURI");
            var valueURINodes = xmlReader.xpathToStream(document, "//ddm:subject/@valueURI");

            var expr = List.of(
                "//*[@xsi:type='dcterms:URI']",
                "//*[@xsi:type='dcterms:URL']",
                "//*[@xsi:type='URI']",
                "//*[@xsi:type='URL']",
                "//*[@scheme='dcterms:URI']",
                "//*[@scheme='dcterms:URL']",
                "//*[@scheme='URI']",
                "//*[@scheme='URL']"
            );

            var elementSelectors = xmlReader.xpathsToStream(document, expr);

            var errors = Stream.of(hrefNodes, schemeURINodes, valueURINodes, elementSelectors)
                .flatMap(i -> i)
                .map(node -> {
                    var value = node.getTextContent();

                    try {
                        var uri = new URI(value);

                        if (!List.of("http", "https").contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
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
                .collect(Collectors.toCollection(ArrayList::new));

            if (errors.size() > 0) {
                throw new RuleViolationDetailsException(errors);
            }
        };
    }

    @Override
    public BagValidatorRule ddmMustHaveRightsHolderDeposit() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

            var rightsHolder= getRightsHolderInElement(document);

            if (rightsHolder.isEmpty()) {
                throw new RuleViolationDetailsException("No rightsholder found in <dcterms:rightsHolder> element");
            }
        };
    }

    @Override
    public BagValidatorRule ddmMustHaveRightsHolderMigration() {
        return (path) -> {
            var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

            var inRole = xmlReader.xpathToStream(document, "//dcx-dai:author/dcx-dai:role")
                .filter(node -> node.getTextContent().equals("RightsHolder"))
                .findFirst();
            var rightsHolder= getRightsHolderInElement(document);

            if (inRole.isEmpty() && rightsHolder.isEmpty()) {
                throw new RuleViolationDetailsException("No RightsHolder found in <dcx-dai:role> element nor in <dcterms:rightsHolder> element");
            }
        };
    }

    private Optional<String> getRightsHolderInElement(Document document) throws XPathExpressionException {
        return xmlReader.xpathToStream(document, "//ddm:dcmiMetadata//dcterms:rightsHolder")
            .map(Node::getTextContent)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .findFirst();
    }

    @Override
    public BagValidatorRule organizationalIdentifierVersionIsValid() {
        return path -> {
            var hasOrganizationalIdentifier = bagItMetadataReader.getField(path, "Has-Organizational-Identifier");

            if (hasOrganizationalIdentifier.isEmpty()) {
                throw new RuleSkipDependenciesException();
            }

            bagInfoContainsAtMostOneOf("Has-Organizational-Identifier-Version").validate(path);
        };
    }

    @Override
    public BagValidatorRule containsNotJustMD5Manifest() {
        return path -> {
            var bag = bagItMetadataReader.getBag(path).orElseThrow();
            var manifests = bagItMetadataReader.getBagManifests(bag);

            var hasOtherManifests = false;

            for (var manifest : manifests) {
                if (!StandardSupportedAlgorithms.MD5.equals(manifest.getAlgorithm())) {
                    hasOtherManifests = true;
                }
            }

            if (!hasOtherManifests) {
                throw new RuleViolationDetailsException("The bag contains no manifests or only a MD5 manifest");
            }
        };
    }
}
