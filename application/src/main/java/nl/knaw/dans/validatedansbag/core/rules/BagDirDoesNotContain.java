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
import nl.knaw.dans.validatedansbag.core.service.FileService;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class BagDirDoesNotContain implements BagValidatorRule {
    private final Path dir;
    private final String[] paths;
    private final FileService fileService;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var basePath = path.resolve(dir);
        var notAllowed = Arrays.stream(paths)
                .map(Path::of)
                .collect(Collectors.toSet());

        var foundButNotAllowedItems = fileService.getAllFilesAndDirectories(basePath)
                .stream()
                .filter(p -> !basePath.equals(p))
                .map(basePath::relativize)
                .filter(notAllowed::contains)
                // filter out the parent path
                .collect(Collectors.toSet());

        log.debug("Found items that are not allowed in path {}: {} ", basePath, foundButNotAllowedItems);

        if (foundButNotAllowedItems.size() > 0) {
            var filenames = foundButNotAllowedItems.stream().map(Path::toString).collect(Collectors.joining(", "));

            return RuleResult.error(String.format(
                    "Directory %s contains files or directories that are not allowed: %s",
                    dir, filenames
            ));
        }

        return RuleResult.ok();
    }
}
