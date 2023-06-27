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
import nl.knaw.dans.validatedansbag.core.service.XmlReader;

import java.nio.file.Path;

@Slf4j
public class DatasetXmlHasRightsHolderInElementOrInAuthorRole extends DatasetXmlHasRightsHolderRuleBase implements BagValidatorRule {

    public DatasetXmlHasRightsHolderInElementOrInAuthorRole(XmlReader xmlReader) {
        super(xmlReader);
    }

    @Override
    public RuleResult validate(Path path) throws Exception {
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));

        var inRole = getRightsHolderInAuthor(document);
        var rightsHolder = getRightsHolderInElement(document);
        log.debug("Results for rights holder search, inRole {}, in rightsHolder element {}", inRole, rightsHolder);

        if (inRole.isEmpty() && rightsHolder.isEmpty()) {
            return RuleResult.error("No RightsHolder found in <dcx-dai:role> nor in <rightsHolder> element");
        }

        return RuleResult.ok();

    }
}
