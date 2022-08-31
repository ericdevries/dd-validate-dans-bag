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

import java.nio.file.Path;
import java.util.List;

public interface OriginalFilepathsService {

    boolean exists(Path bagDir);

    List<OriginalFilePathItem> getMapping(Path bagDir);

    class OriginalFilePathItem {
        private final Path originalFilename;
        private final Path renamedFilename;
        public OriginalFilePathItem(Path originalFilename, Path renamedFilename) {
            this.originalFilename = originalFilename;
            this.renamedFilename = renamedFilename;
        }

        public Path getOriginalFilename() {
            return originalFilename;
        }

        public Path getRenamedFilename() {
            return renamedFilename;
        }

    }
}
