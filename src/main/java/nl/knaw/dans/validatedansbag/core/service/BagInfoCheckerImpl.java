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
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BagInfoCheckerImpl implements BagInfoChecker {
    private static final Logger log = LoggerFactory.getLogger(BagInfoCheckerImpl.class);

    private final FileService fileService;

    private final BagItMetadataReader bagItMetadataReader;
    private final BagXmlReader bagXmlReader;

    private final OriginalFilepathsService originalFilepathsService;

    private final Pattern doiPattern = Pattern.compile("^10(\\.\\d+)+/.+");

    private final Pattern doiUrlPattern = Pattern.compile("^((https?://(dx\\.)?)?doi\\.org/(urn:)?(doi:)?)?10(\\.\\d+)+/.+");
    private final Pattern urnPattern = Pattern.compile("^urn:[A-Za-z0-9][A-Za-z0-9-]{0,31}:[a-z0-9()+,\\-\\\\.:=@;$_!*'%/?#]+$");

    private final String daiPrefix = "info:eu-repo/dai/nl/";

    private final DaiDigestCalculator daiDigestCalculator;

    private final PolygonListValidator polygonListValidator;

    private final XmlValidator xmlValidator;

    private final String filesXmlNamespace = "http://easy.dans.knaw.nl/schemas/bag/metadata/files/";

    private final String namespaceDcterms = "http://purl.org/dc/terms/";
    private final Set<String> allowedFilesXmlNamespaces = Set.of(
        "http://purl.org/dc/terms/",
        "http://purl.org/dc/elements/1.1/"
    );
    private final Set<String> validLicenses = Set.of(
        "http://creativecommons.org/licenses/by-nc-nd/4.0/",
        "http://creativecommons.org/licenses/by-nc-sa/3.0",
        "http://creativecommons.org/licenses/by-nc-sa/4.0/",
        "http://creativecommons.org/licenses/by-nc/3.0",
        "http://creativecommons.org/licenses/by-nc/4.0/",
        "http://creativecommons.org/licenses/by-nd/4.0/",
        "http://creativecommons.org/licenses/by-sa/4.0/",
        "http://creativecommons.org/licenses/by/4.0",
        "http://creativecommons.org/publicdomain/zero/1.0",
        "http://opendatacommons.org/licenses/by/1-0/index.html",
        "http://opensource.org/licenses/BSD-2-Clause",
        "http://opensource.org/licenses/BSD-3-Clause",
        "http://opensource.org/licenses/MIT",
        "http://www.apache.org/licenses/LICENSE-2.0",
        "http://www.cecill.info/licences/Licence_CeCILL-B_V1-en.html",
        "http://www.cecill.info/licences/Licence_CeCILL_V2-en.html",
        "http://www.gnu.org/licenses/gpl-3.0.en.html",
        "http://www.gnu.org/licenses/lgpl-3.0.txt",
        "http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html",
        "http://www.mozilla.org/en-US/MPL/2.0/FAQ/",
        "http://www.ohwr.org/attachments/2388/cern_ohl_v_1_2.txt",
        "http://www.ohwr.org/attachments/735/CERNOHLv1_1.txt",
        "http://www.ohwr.org/projects/cernohl/wiki",
        "http://www.tapr.org/TAPR_Open_Hardware_License_v1.0.txt",
        "http://www.tapr.org/ohl.html",
        "http://dans.knaw.nl/en/about/organisation-and-policy/legal-information/DANSGeneralconditionsofuseUKDEF.pdf",
        "http://dans.knaw.nl/en/about/organisation-and-policy/legal-information/DANSLicence.pdf"
    );

    private final Set<String> allowedAccessRights = Set.of("ANONYMOUS", "RESTRICTED_REQUEST", "NONE");

    public BagInfoCheckerImpl(FileService fileService, BagItMetadataReader bagItMetadataReader, BagXmlReader bagXmlReader, OriginalFilepathsService originalFilepathsService,
        DaiDigestCalculator daiDigestCalculator,
        PolygonListValidator polygonListValidator, XmlValidator xmlValidator) {
        this.fileService = fileService;
        this.bagItMetadataReader = bagItMetadataReader;
        this.bagXmlReader = bagXmlReader;
        this.originalFilepathsService = originalFilepathsService;
        this.daiDigestCalculator = daiDigestCalculator;
        this.polygonListValidator = polygonListValidator;
        this.xmlValidator = xmlValidator;
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
                    throw new RuleViolationDetailsException(String.format(
                        "Date '%s' is not valid", created
                    ), e);
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected error occurred", e);
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
                else {
                    throw new RuleSkippedException();
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
            try {
                var mapping = originalFilepathsService.getMapping(path);

                var document = bagXmlReader.readXmlFile(path.resolve("metadata/files.xml"));
                var searchExpressions = List.of(
                    "/files:files/files:file/@filepath",
                    "/files/file/@filepath");

                // the files defined in metadata/files.xml
                var fileXmlPaths = searchExpressions.stream().map(e -> {
                        try {
                            return bagXmlReader.xpathToStream(document, e);
                        }
                        catch (XPathExpressionException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .flatMap(i -> i)
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
            }
            catch (Exception e) {
                log.error("Unexpected error occurred", e);
                throw new RuleViolationDetailsException("Unexpected error occurred", e);
            }
        };
    }

    @Override
    public BagValidatorRule ddmMayContainDctermsLicenseFromList() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
                var expr = "//ddm:dcmiMetadata/dcterms:license[@xsi:type]";

                var nodes = bagXmlReader.xpathToStream(document, expr).collect(Collectors.toList());

                if (nodes.size() == 0) {
                    throw new RuleViolationDetailsException("No licenses found");
                }

                var node = nodes.get(0);
                var license = nodes.get(0).getTextContent();
                var attr = node.getAttributes().getNamedItem("xsi:type").getTextContent();

                // converts a namespace uri into a prefix that is used in the document
                var prefix = document.lookupPrefix(namespaceDcterms);

                if (!attr.equals(String.format("%s:URI", prefix))) {
                    throw new RuleViolationDetailsException("No license with xsi:type=\"dcterms:URI\"");
                }

                // strip trailing slashes so url's are more consistent
                var licenses = validLicenses.stream().map(l -> l.replaceAll("/+$", "")).collect(Collectors.toSet());

                if (!licenses.contains(license)) {
                    throw new RuleViolationDetailsException(String.format(
                        "Found unknown or unsupported license: %s", license
                    ));
                }
            }
            catch (Exception e) {
                log.error("Error reading dataset.xml file", e);
                throw new RuleViolationDetailsException("Unexpected exception occurred while processing", e);
            }
        };
    }

    @Override
    public BagValidatorRule ddmContainsUrnNbnIdentifier() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
                var expr = "//dcterms:identifier[@xsi:type=\"id-type:URN\"]";

                var nodes = bagXmlReader.xpathToStream(document, expr);

                nodes.filter((node) -> node.getTextContent().contains("urn:nbn"))
                    .findFirst()
                    .orElseThrow(() -> new RuleViolationDetailsException("URN:NBN identifier is missing"));
            }
            catch (Exception e) {
                log.error("Error reading dataset.xml file", e);
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

                var nodes = bagXmlReader.xpathToStream(document, expr);
                var match = nodes.filter((node) -> {
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
                var match = bagXmlReader.xpathToStream(document, expr)
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
                var nodes = bagXmlReader.xpathToStream(document, expr);

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
                var nodes = bagXmlReader.xpathToStream(document, expr);
                var match = nodes.filter(node -> {
                        try {
                            var srsNames = bagXmlReader.xpathToStream(node, ".//*[local-name() = 'Polygon']")
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
                var expr = "//gml:Point | //gml:lowerCorner | //gml:upperCorner";
                var match = bagXmlReader.xpathToStream(document, expr).collect(Collectors.toList());

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
                var match = bagXmlReader.xpathToStream(document, expr)
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

    @Override
    public BagValidatorRule allUrlsAreValid() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

                var hrefNodes = bagXmlReader.xpathToStream(document, "*/@href");
                var schemeURINodes = bagXmlReader.xpathToStream(document, "//ddm:subject/@schemeURI");
                var valueURINodes = bagXmlReader.xpathToStream(document, "//ddm:subject/@valueURI");

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

                var elementSelectors = bagXmlReader.xpathsToStream(document, expr);

                var doiValues = bagXmlReader.xpathsToStream(document, List.of("//*[@scheme = 'id-type:DOI']", "//*[@scheme = 'DOI']"))
                    .filter(node -> node.getAttributes().getNamedItem("href") == null);

                var urnValues = bagXmlReader.xpathsToStream(document, List.of("//*[@scheme = 'id-type:URN']", "//*[@scheme = 'URN']"))
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
                var inRole = bagXmlReader.xpathToStream(document, "//*[local-name() = 'role']")
                    .filter(node -> node.getTextContent().contains("rightsholder"))
                    .findFirst();

                var rightsHolder = bagXmlReader.xpathToStream(document, "//ddm:dcmiMetadata//dcterms:rightsHolder")
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

    private void validateXmlFile(Path file, String schema) throws RuleViolationDetailsException {
        try {
            var document = bagXmlReader.readXmlFile(file);
            var results = xmlValidator.validateDocument(document, schema);

            if (results.size() > 0) {
                var errorList = results.stream()
                    .map(Throwable::getLocalizedMessage)
                    .map(e -> String.format(" - %s", e))
                    .collect(Collectors.joining("\n"));

                // TODO see how we can get all the errors in there
                throw new RuleViolationDetailsException(String.format(
                    "%s does not confirm to %s: \n%s", file, schema, errorList
                ));
            }
        }
        catch (Exception e) {
            throw new RuleViolationDetailsException(String.format(
                "%s does not confirm to %s", file, schema
            ), e);
        }
    }

    @Override
    public BagValidatorRule xmlFileConfirmsToSchema(Path file, String schema) {
        return (path) -> {
            validateXmlFile(path.resolve(file), schema);
        };
    }

    @Override
    public BagValidatorRule xmlFileIfExistsConformsToSchema(Path file, String schema) {
        return (path) -> {
            var fileName = path.resolve(file);

            if (fileService.exists(fileName)) {
                validateXmlFile(fileName, schema);
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlHasDocumentElementFiles() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/files.xml"));
                var rootNode = document.getDocumentElement();

                if (rootNode == null || !"files".equals(rootNode.getNodeName())) {
                    throw new RuleViolationDetailsException("files.xml document element must be 'files'");
                }
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Error reading files.xml", e);
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlHasOnlyFiles() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/files.xml"));
                var namespace = document.getNamespaceURI();

                if (filesXmlNamespace.equals(namespace)) {
                    log.debug("Rule filesXmlHasOnlyFiles has been checked by files.xsd");
                }
                else {
                    var nonFiles = bagXmlReader.xpathToStream(document, "/files/*[local-name() != 'file']")
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
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Error reading files.xml", e);
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlFileElementsAllHaveFilepathAttribute() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/files.xml"));

                var missingAttributes = bagXmlReader.xpathToStream(document, "/files/file")
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
            catch (Exception e) {
                throw new RuleViolationDetailsException("Error reading files.xml", e);
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFiles() {
        /// TODO implement prestaged file logics
        return (path) -> {
            try {
                var dataPath = path.resolve("data");
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/files.xml"));

                if (originalFilepathsService.exists(path)) {
                    log.debug("original-filepaths.txt exists, so checking is not needed");
                    return;
                }

                var searchExpressions = List.of(
                    "/files:files/files:file/@filepath",
                    "/files/file/@filepath");

                var filePathNodes = bagXmlReader.xpathsToStream(document, searchExpressions).collect(Collectors.toList());

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
            }
            catch (Exception e) {
                e.printStackTrace();
                throw new RuleViolationDetailsException("Error reading files.xml", e);
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlAllFilesHaveFormat() {
        return (path) -> {

            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/files.xml"));
                var expr = "//file";

                var wrongNodes = bagXmlReader.xpathToStream(document, expr).filter(node -> {
                    try {
                        var size = bagXmlReader.xpathToStream(node, ".//dcterms:format").collect(Collectors.toSet()).size();

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
            }
            catch (Exception e) {
                log.error("Error reading files.xml", e);
                throw new RuleViolationDetailsException("Error reading files.xml", e);
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlFilesHaveOnlyAllowedNamespaces() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/files.xml"));

                if (filesXmlNamespace.equals(document.getNamespaceURI())) {
                    log.debug("Rule filesXmlFilesHaveOnlyAllowedNamespaces has been checked by files.xsd");
                }

                var errors = bagXmlReader.xpathToStream(document, "//file/*")
                    .filter(node -> !allowedFilesXmlNamespaces.contains(node.getNamespaceURI()))
                    .collect(Collectors.toList());

                if (errors.size() > 0) {
                    throw new RuleViolationDetailsException("files.xml: non-dc/dcterms elements found in some file elements");
                }
            }
            catch (Exception e) {
                log.error("Error reading files.xml", e);
                throw new RuleViolationDetailsException("Error reading files.xml", e);
            }
        };
    }

    @Override
    public BagValidatorRule filesXmlFilesHaveOnlyAllowedAccessRights() {
        return (path) -> {
            try {
                var document = bagXmlReader.readXmlFile(path.resolve("metadata/files.xml"));

                var invalidNodes = bagXmlReader.xpathToStream(document, "//file/dcterms:accessRights")
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

            }
            catch (Exception e) {
                log.error("Error reading files.xml", e);
                throw new RuleViolationDetailsException("Error reading files.xml", e);
            }
        };
    }
}
