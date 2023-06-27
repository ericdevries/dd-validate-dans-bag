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
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.FileService;

import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class BagInfoExistsAndIsWellformed implements BagValidatorRule {
    private final BagItMetadataReader bagItMetadataReader;
    private final FileService fileService;

    @Override
    public RuleResult validate(Path path) throws Exception {
        if (!fileService.isFile(path.resolve(Path.of("bag-info.txt")))) {
            return RuleResult.error("bag-info.txt does not exist");
        }

        try {
            log.debug("Reading bag metadata from {}", path);
            bagItMetadataReader.getBag(path).orElseThrow();
            return RuleResult.ok();
        } catch (Exception e) {
            return RuleResult.error(String.format(
                    "bag-info.txt exists but is malformed: %s", e.getMessage()
            ), e);
        }
    }
 }
