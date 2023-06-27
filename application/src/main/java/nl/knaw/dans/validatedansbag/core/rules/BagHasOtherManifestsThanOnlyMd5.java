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

import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.validatedansbag.core.BagNotFoundException;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;

import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class BagHasOtherManifestsThanOnlyMd5 implements BagValidatorRule {
    private final BagItMetadataReader bagItMetadataReader;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var bag = bagItMetadataReader.getBag(path).orElseThrow(
                () -> new BagNotFoundException(String.format("Bag on path %s could not be opened", path)));

        var manifests = bagItMetadataReader.getBagManifests(bag);
        var hasOtherManifests = false;

        log.debug("Manifests to compare: {}", manifests);

        for (var manifest : manifests) {
            log.trace("Checking if manifest {} has MD5 algorithm (algorithm is {})", manifest, manifest.getAlgorithm());

            if (!StandardSupportedAlgorithms.MD5.equals(manifest.getAlgorithm())) {
                hasOtherManifests = true;
                break;
            }
        }

        if (!hasOtherManifests) {
            return RuleResult.error("The bag contains no manifests or only a MD5 manifest");
        }

        return RuleResult.ok();
    }
}
