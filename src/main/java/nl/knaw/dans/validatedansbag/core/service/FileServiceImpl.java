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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FileServiceImpl implements FileService {
    @Override
    public boolean isDirectory(Path path) {
        return Files.exists(path) && Files.isDirectory(path);
    }

    @Override
    public boolean isFile(Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    @Override
    public List<Path> getAllFiles(Path path) throws IOException {
        try (var stream = Files.walk(path)) {
            return stream.filter(Files::isRegularFile).collect(Collectors.toList());
        }
    }

    @Override
    public List<Path> getAllFilesAndDirectories(Path path) throws IOException {
        try (var stream = Files.walk(path)) {
            return stream.collect(Collectors.toList());
        }
    }

    @Override
    public byte[] readFileContents(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public boolean exists(Path path) {
        return Files.exists(path);
    }
}
