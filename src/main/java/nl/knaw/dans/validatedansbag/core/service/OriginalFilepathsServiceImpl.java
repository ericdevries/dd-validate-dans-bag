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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OriginalFilepathsServiceImpl implements OriginalFilepathsService {

    private static final Logger log = LoggerFactory.getLogger(OriginalFilepathsServiceImpl.class);

    private final FileService fileService;
    private final String filename = "original-filepaths.txt";

    public OriginalFilepathsServiceImpl(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public List<OriginalFilePathItem> getMapping(Path bagDir) {
        var file = bagDir.resolve(filename);

        try {
            var bytes = fileService.readFileContents(file);
            var content = new String(bytes);

            // the mapping between files on disk and what they used to be called
            return Arrays.stream(content.split("\n"))
                .filter(s -> !s.isBlank())
                .map(s -> s.split("\\s+", 2))
                .filter(p -> p.length == 2)
                .map(p -> new OriginalFilePathItem(Path.of(p[1]), Path.of(p[0])))
                .collect(Collectors.toList());
        }
        catch (NoSuchFileException e) {
            log.debug("File {} not found", file);
        }
        catch (Exception e) {
            log.error("Error while reading {}", file, e);
        }

        return List.of();
    }

    @Override
    public Map<Path, Path> getMappingsFromOriginalToRenamed(Path bagDir) {
        var mappings = getMapping(bagDir);
        var result = new HashMap<Path, Path>();

        for (var m : mappings) {
            result.put(m.getOriginalFilename(), m.getRenamedFilename());
        }

        return result;
    }

    @Override
    public boolean exists(Path path) {
        return fileService.exists(path.resolve(filename));
    }
}
