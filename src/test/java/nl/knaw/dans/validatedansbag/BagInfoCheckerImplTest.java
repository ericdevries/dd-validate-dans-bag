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

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BagInfoCheckerImplTest {

    final BagXmlReader bagXmlReader = Mockito.mock(BagXmlReader.class);

    @AfterEach
    void afterEach() {
        Mockito.reset(bagXmlReader);
    }

    @Test
    void testBagIsValid() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        assertDoesNotThrow(() -> checker.bagIsValid().validate(Path.of("testpath")));

        Mockito.verify(bagItMetadataReader).verifyBag(Path.of("testpath"));

    }

    @Test
    void testBagIsNotValidWithExceptionThrown() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.doThrow(new InvalidBagitFileFormatException("Invalid file format"))
            .when(bagItMetadataReader).verifyBag(Mockito.any());

        assertThrows(RuleViolationDetailsException.class, () -> checker.bagIsValid().validate(Path.of("testpath")));
    }

    @Test
    void containsDirWorks() throws RuleViolationDetailsException {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.isDirectory(Mockito.any()))
            .thenReturn(true);

        checker.containsDir(Path.of("testpath")).validate(Path.of("bagdir"));

        Mockito.verify(fileService).isDirectory(Path.of("bagdir/testpath"));
    }

    @Test
    void containsDirThrowsException() {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.isDirectory(Mockito.any()))
            .thenReturn(false);

        assertThrows(RuleViolationDetailsException.class, () -> checker.containsDir(Path.of("testpath")).validate(Path.of("bagdir")));
    }

    @Test
    void containsFileWorks() throws RuleViolationDetailsException {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.isFile(Mockito.any()))
            .thenReturn(true);

        checker.containsFile(Path.of("testpath")).validate(Path.of("bagdir"));

        Mockito.verify(fileService).isFile(Path.of("bagdir/testpath"));
    }

    @Test
    void containsFileThrowsException() {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.isFile(Mockito.any()))
            .thenReturn(false);

        assertThrows(RuleViolationDetailsException.class, () -> checker.containsFile(Path.of("testpath")).validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoExistsAndIsWellFormed() {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.isFile(Mockito.any()))
            .thenReturn(true);

        Mockito.when(bagItMetadataReader.getBag(Mockito.any()))
                .thenReturn(Optional.of(new Bag()));

        assertDoesNotThrow(() -> checker.bagInfoExistsAndIsWellFormed().validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoDoesNotExist() {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.isFile(Mockito.any()))
            .thenReturn(false);

        assertThrows(RuleViolationDetailsException.class, () -> checker.bagInfoExistsAndIsWellFormed().validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoDoesExistButItCouldNotBeOpened() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.isFile(Mockito.any()))
            .thenReturn(true);

        Mockito.when(bagItMetadataReader.getBag(Mockito.any()))
                .thenReturn(Optional.empty());

        assertThrows(RuleViolationDetailsException.class, () -> checker.bagInfoExistsAndIsWellFormed().validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoCreatedElementIsIso8601Date() throws Exception {

        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Created")))
            .thenReturn(List.of("2022-01-01T01:23:45.678+00:00"));

        assertDoesNotThrow(() -> checker.bagInfoCreatedElementIsIso8601Date().validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoCreatedElementIsNotAValidDate() throws Exception {

        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Created")))
            .thenReturn(List.of("2022-01-01 01:23:45.678"));

        assertThrows(RuleViolationDetailsException.class, () -> checker.bagInfoCreatedElementIsIso8601Date().validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoContainsExactlyOneOf() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Key")))
            .thenReturn(List.of("value"));

        assertDoesNotThrow(() -> checker.bagInfoContainsExactlyOneOf("Key").validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoContainsExactlyOneOfButInRealityItIsTwo() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Key")))
            .thenReturn(List.of("value", "secondvalue"));

        assertThrows(RuleViolationDetailsException.class, () -> checker.bagInfoContainsExactlyOneOf("Key").validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoContainsExactlyOneOfButInRealityItIsZero() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Key")))
            .thenReturn(new ArrayList<>());

        assertThrows(RuleViolationDetailsException.class, () -> checker.bagInfoContainsExactlyOneOf("Key").validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoContainsAtMostOne() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Key")))
            .thenReturn(List.of("value"));

        assertDoesNotThrow(() -> checker.bagInfoContainsAtMostOneOf("Key").validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoContainsAtMostOneButItReturnsTwo() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Key")))
            .thenReturn(List.of("value", "secondvalue"));

        assertThrows(RuleViolationDetailsException.class, () -> checker.bagInfoContainsAtMostOneOf("Key").validate(Path.of("bagdir")));
    }

    @Test
    void bagInfoContainsAtMostOneOfButInRealityItIsZero() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);
        Mockito.when(bagItMetadataReader.getField(Mockito.any(), Mockito.eq("Key")))
            .thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> checker.bagInfoContainsAtMostOneOf("Key").validate(Path.of("bagdir")));
    }

    @Test
    void bagShaPayloadManifestContainsAllPayloadFiles() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.getAllFiles(Mockito.any()))
            .thenReturn(List.of(Path.of("path/1.txt"), Path.of("path/2.txt")));

        var bag = new Bag();
        var manifest = new Manifest(StandardSupportedAlgorithms.SHA1);
        manifest.setFileToChecksumMap(Map.of(
            Path.of("path/1.txt"), "checksum1",
            Path.of("path/2.txt"), "checksum2"
        ));

        bag.setPayLoadManifests(Set.of(manifest));

        Mockito.when(bagItMetadataReader.getBag(Mockito.any())).thenReturn(Optional.of(bag));
        Mockito.when(bagItMetadataReader.getBagManifest(Mockito.any(), Mockito.any()))
            .thenReturn(Optional.of(manifest));

        assertDoesNotThrow(() -> checker.bagShaPayloadManifestContainsAllPayloadFiles().validate(Path.of("bagdir")));
    }

    @Test
    void bagShaPayloadManifestMissesSomeFiles() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.getAllFiles(Mockito.any()))
            .thenReturn(List.of(Path.of("path/1.txt"), Path.of("path/2.txt"), Path.of("path/3.txt")));

        var bag = new Bag();
        var manifest = new Manifest(StandardSupportedAlgorithms.SHA1);
        manifest.setFileToChecksumMap(Map.of(
            Path.of("path/1.txt"), "checksum1",
            Path.of("path/2.txt"), "checksum2"
        ));

        bag.setPayLoadManifests(Set.of(manifest));

        Mockito.when(bagItMetadataReader.getBag(Mockito.any())).thenReturn(Optional.of(bag));

        assertThrows(RuleViolationDetailsException.class, () -> checker.bagShaPayloadManifestContainsAllPayloadFiles().validate(Path.of("bagdir")));
    }

    @Test
    void bagShaPayloadManifestHasTooManyFiles() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        Mockito.when(fileService.getAllFiles(Mockito.any()))
            .thenReturn(List.of(Path.of("path/1.txt"), Path.of("path/2.txt")));

        var bag = new Bag();
        var manifest = new Manifest(StandardSupportedAlgorithms.SHA1);
        manifest.setFileToChecksumMap(Map.of(
            Path.of("path/1.txt"), "checksum1",
            Path.of("path/2.txt"), "checksum2",
            Path.of("path/3.txt"), "checksum3"
        ));

        bag.setPayLoadManifests(Set.of(manifest));

        Mockito.when(bagItMetadataReader.getBag(Mockito.any())).thenReturn(Optional.of(bag));

        assertThrows(RuleViolationDetailsException.class, () -> checker.bagShaPayloadManifestContainsAllPayloadFiles().validate(Path.of("bagdir")));
    }

    @Test
    void containsNothingElseThan() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);
        var basePath = Path.of("bagdir/metadata");

        Mockito.when(fileService.getAllFilesAndDirectories(Mockito.eq(basePath)))
            .thenReturn(List.of(basePath.resolve("1.txt"), basePath.resolve("2.txt")));

        assertDoesNotThrow(() -> {
            checker.containsNothingElseThan(Path.of("metadata"), new String[] {
                "1.txt",
                "2.txt",
                "3.txt"
            }).validate(Path.of("bagdir"));
        });
    }

    @Test
    void containsNothingElseThanButThereAreInvalidFiles() throws Exception {
        var fileService = Mockito.mock(FileService.class);
        var bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);
        var checker = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader);

        var basePath = Path.of("bagdir/metadata");

        Mockito.when(fileService.getAllFilesAndDirectories(Mockito.eq(basePath)))
            .thenReturn(List.of(basePath.resolve("1.txt"), basePath.resolve("2.txt"), basePath.resolve("oh no.txt")));

        assertThrows(RuleViolationDetailsException.class, () -> {
            checker.containsNothingElseThan(Path.of("metadata"), new String[] {
                "1.txt"
                ,
                "2.txt",
                "3.txt"
            }).validate(Path.of("bagdir"));
        });
    }
}
