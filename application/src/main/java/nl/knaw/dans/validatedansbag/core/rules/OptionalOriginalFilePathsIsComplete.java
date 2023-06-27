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
import nl.knaw.dans.validatedansbag.core.service.FilesXmlService;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsService;
import org.apache.commons.collections4.CollectionUtils;

import java.nio.file.Path;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class OptionalOriginalFilePathsIsComplete implements BagValidatorRule {
    private final OriginalFilepathsService originalFilepathsService;
    private final FileService fileService;
    private final FilesXmlService filesXmlService;

    @Override
    public RuleResult validate(Path path) throws Exception {
        if (!originalFilepathsService.exists(path)) {
            return RuleResult.skipDependencies();
        }

        var mapping = originalFilepathsService.getMapping(path);

        // the files defined in metadata/files.xml
        var fileXmlPaths = filesXmlService.readFilepaths(path)
                .collect(Collectors.toSet());

        log.trace("Paths in files.xml: {}", fileXmlPaths);

        // the files on disk
        var dataPath = path.resolve("data");
        var actualFiles = fileService.getAllFiles(dataPath)
                .stream()
                .filter(i -> !dataPath.equals(i))
                .map(path::relativize)
                .collect(Collectors.toSet());

        log.trace("Paths inside {}: {}", dataPath, fileXmlPaths);

        var renamedFiles = mapping.stream().map(OriginalFilepathsService.OriginalFilePathItem::getRenamedFilename).collect(Collectors.toSet());
        var originalFiles = mapping.stream().map(OriginalFilepathsService.OriginalFilePathItem::getOriginalFilename).collect(Collectors.toSet());

        // disjunction returns the difference between 2 sets
        // so {1,2,3} disjunction {2,3,4} would return {1,4}
        var physicalFileSetsDiffer = CollectionUtils.disjunction(actualFiles, renamedFiles).size() > 0;
        log.trace("Disjunction between files on disk and files referenced in original-filepaths.txt: {}", physicalFileSetsDiffer);

        var originalFileSetsDiffer = CollectionUtils.disjunction(fileXmlPaths, originalFiles).size() > 0;
        log.trace("Disjunction between files.xml and files referenced in original-filepaths.txt: {}", originalFiles);

        if (physicalFileSetsDiffer || originalFileSetsDiffer) {
            log.debug("File sets are not equal, physicalFileSetsDiffer = {} and originalFileSetsDiffer = {}", physicalFileSetsDiffer, originalFileSetsDiffer);

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

            return RuleResult.error(String.format(
                    "original-filepaths.txt errors: \n%s", message
            ));
        }

        return RuleResult.ok();    }
}
