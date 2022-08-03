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
package nl.knaw.dans.validatedansbag;

import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class BagInfoCheckerImpl implements BagInfoChecker {
    private static final Logger log = LoggerFactory.getLogger(BagInfoCheckerImpl.class);

    private final FileService fileService;

    private final BagItMetadataReader bagItMetadataReader;

    public BagInfoCheckerImpl(FileService fileService, BagItMetadataReader bagItMetadataReader) {
        this.fileService = fileService;
        this.bagItMetadataReader = bagItMetadataReader;
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
                bagItMetadataReader.getBag(path);
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

    @Override
    public BagValidatorRule bagShaPayloadManifestContainsAllPayloadFiles() {
        return (path) -> {
            try {
                var bag = bagItMetadataReader.getBag(path);

                var manifest = bag.getPayLoadManifests().stream()
                    .filter(m -> m.getAlgorithm().equals(StandardSupportedAlgorithms.SHA1))
                    .findFirst()
                    .orElseThrow(() -> new RuleViolationDetailsException("No manifest file found"));

                var filesInManifest = manifest.getFileToChecksumMap().keySet().stream().map(path::relativize).collect(Collectors.toSet());
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
            catch (RuleViolationDetailsException e) {
                throw e;
            }
            catch (Exception e) {
                throw new RuleViolationDetailsException("Unexpected error occurred", e);
            }
        };
    }
}
