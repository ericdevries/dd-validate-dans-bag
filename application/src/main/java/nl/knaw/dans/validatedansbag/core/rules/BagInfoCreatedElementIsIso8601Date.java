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
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class BagInfoCreatedElementIsIso8601Date implements BagValidatorRule {
    private final BagItMetadataReader bagItMetadataReader;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var created = bagItMetadataReader.getSingleField(path, "Created");

        try {
            log.debug("Trying to parse date {} to see if it is valid", created);
            DateTime.parse(created, ISODateTimeFormat.dateTime());
            return RuleResult.ok();
        } catch (Throwable e) {
            return RuleResult.error(String.format(
                    "Date '%s' is not valid", created
            ), e);
        }
    }
}
