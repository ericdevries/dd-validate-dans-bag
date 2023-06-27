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
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidator;

import java.nio.file.Path;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class DatasetXmlContainsExactlyOneDctermsLicenseWithXsiTypeUri implements BagValidatorRule {
    private final XmlReader xmlReader;
    private final LicenseValidator licenseValidator;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
        // converts a namespace uri into a prefix that is used in the document
        var prefix = document.lookupPrefix(XmlReader.NAMESPACE_DCTERMS);
        var expr = String.format("/ddm:DDM/ddm:dcmiMetadata/dcterms:license[@xsi:type='%s:URI']", prefix);

        var validNodes = xmlReader.xpathToStream(document, expr)
                .filter(item -> licenseValidator.isValidUri(item.getTextContent()))
                .collect(Collectors.toList());

        log.debug("Found {} nodes with correct licenses", validNodes.size());

        var numLicensesFound = validNodes.size();

        if (numLicensesFound == 1) {
            return RuleResult.ok();
        } else if (numLicensesFound == 0) {
            return RuleResult.error("No license with xsi:type=\"dcterms:URI\"");
        } else {
            return RuleResult.error("More than one license with xsi:type=\"dcterms:URI\"");
        }
    }
}
