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

import org.apache.commons.io.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

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

    @Override
    public boolean isReadable(Path path) {
        return Files.exists(path) && Files.isReadable(path);
    }

    @Override
    public CharBuffer readFileContents(Path path, Charset charset) throws IOException {
        var contents = readFileContents(path);
        return charset.newDecoder().decode(ByteBuffer.wrap(contents));
    }

    @Override
    public Path extractZipFile(InputStream inputStream) throws IOException {
        var tempPath = Files.createTempDirectory("bag-");

        try (var input = new ZipInputStream(inputStream)) {
            var entry = input.getNextEntry();

            while (entry != null) {
                var targetPath = tempPath.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                }
                else {
                    writeStreamToFile(input, targetPath);
                }

                entry = input.getNextEntry();
            }
        }

        return tempPath;
    }

    @Override
    public void deleteDirectoryAndContents(Path path) throws IOException {
        FileUtils.deleteDirectory(path.toFile());
    }

    @Override
    public Optional<Path> getFirstDirectory(Path path) throws IOException {
        try (var s = Files.walk(path)) {
            return s.filter(this::isDirectory).skip(1).findFirst();
        }
    }

    void writeStreamToFile(InputStream inputStream, Path target) throws IOException {

        try (var output = new FileOutputStream(target.toFile())) {
            byte[] buf = new byte[8 * 1024];
            var bytesRead = 0;

            while ((bytesRead = inputStream.read(buf)) != -1) {
                output.write(buf, 0, bytesRead);
            }
        }

    }
}
