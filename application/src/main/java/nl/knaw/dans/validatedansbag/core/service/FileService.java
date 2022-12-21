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
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface FileService {

    boolean isDirectory(Path path);

    boolean isFile(Path path);

    List<Path> getAllFiles(Path path) throws IOException;

    List<Path> getAllFilesAndDirectories(Path path) throws IOException;

    byte[] readFileContents(Path path) throws IOException;

    boolean exists(Path path);
    boolean isReadable(Path path);

    CharBuffer readFileContents(Path path, Charset charset) throws IOException;

    Path extractZipFile(InputStream inputStream) throws IOException;

    void deleteDirectoryAndContents(Path path) throws IOException;

    Optional<Path> getFirstDirectory(Path path) throws IOException;
}
