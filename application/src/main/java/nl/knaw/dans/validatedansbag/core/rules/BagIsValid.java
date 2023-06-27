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
import gov.loc.repository.bagit.exceptions.FileNotInManifestException;
import gov.loc.repository.bagit.exceptions.FileNotInPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MissingBagitFileException;
import gov.loc.repository.bagit.exceptions.MissingPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.MissingPayloadManifestException;
import gov.loc.repository.bagit.exceptions.VerificationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class BagIsValid implements BagValidatorRule {
    private final BagItMetadataReader bagItMetadataReader;


    @Override
    public RuleResult validate(Path path) throws Exception {
        try {
            log.debug("Verifying bag {}", path);
            bagItMetadataReader.verifyBag(path);
            log.debug("Bag {} is valid", path);
            return RuleResult.ok();
        }
        // only catch exceptions that have to do with the bag verification;
        // other exceptions such as IOException should be propagated to the rule engine
        // sadly FileNotInManifestException bubbles up as an IOException
        catch (FileNotInManifestException | InvalidBagitFileFormatException | MissingPayloadManifestException |
               MissingPayloadDirectoryException | FileNotInPayloadDirectoryException | MissingBagitFileException |
               CorruptChecksumException | VerificationException | NoSuchFileException e) {

            return RuleResult.error(String.format(
                    "Bag is not valid: %s", e.getMessage()
            ), e);
        }
    }
}
