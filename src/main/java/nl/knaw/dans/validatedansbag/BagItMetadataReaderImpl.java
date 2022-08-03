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
import gov.loc.repository.bagit.exceptions.CorruptChecksumException;
import gov.loc.repository.bagit.exceptions.FileNotInPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.MissingBagitFileException;
import gov.loc.repository.bagit.exceptions.MissingPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.MissingPayloadManifestException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.exceptions.VerificationException;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class BagItMetadataReaderImpl implements BagItMetadataReader {
    private static final Logger log = LoggerFactory.getLogger(BagItMetadataReaderImpl.class);

    @Override
    public Bag getBag(Path path) throws MaliciousPathException, UnparsableVersionException, UnsupportedAlgorithmException, InvalidBagitFileFormatException, IOException {
        return new BagReader().read(path);
    }

    @Override
    public void verifyBag(Path path)
        throws MaliciousPathException, UnparsableVersionException, UnsupportedAlgorithmException, InvalidBagitFileFormatException, IOException, MissingPayloadManifestException,
        MissingPayloadDirectoryException, FileNotInPayloadDirectoryException, InterruptedException, MissingBagitFileException, CorruptChecksumException, VerificationException {

        try (var verifier = new BagVerifier()) {
            var bag = getBag(path);
            var ignoreHiddenFiles = false;

            log.trace("Verifying bag is complete on path {}", path);
            verifier.isComplete(bag, ignoreHiddenFiles);

            log.trace("Verifying bag is valid on path {}", path);
            verifier.isValid(bag, ignoreHiddenFiles);
        }
    }

    @Override
    public List<String> getField(Path bagDir, String field) throws MaliciousPathException, UnparsableVersionException, UnsupportedAlgorithmException, InvalidBagitFileFormatException, IOException {
        var bag = getBag(bagDir);
        return bag.getMetadata().get(field);
    }
}
