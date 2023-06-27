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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import nl.knaw.dans.validatedansbag.core.service.XmlSchemaValidator;

import java.nio.file.Path;

@Slf4j
public class OptionalBagFileConformsToXmlSchema extends BagFileConformsToXmlSchema {
    private final FileService fileService;

    public OptionalBagFileConformsToXmlSchema(Path file, XmlReader reader, String schema, XmlSchemaValidator validator, FileService fileService) {
        super(file, reader, schema, validator);
        this.fileService = fileService;
    }

    @Override
    public RuleResult validate(Path path) throws Exception {
        var fileName = path.resolve(file);

        if (fileService.exists(fileName)) {
            log.debug("Validating {} against schema {}", fileName, schema);
            return super.validate(path);
        } else {
            return RuleResult.skipDependencies();
        }
    }
}
