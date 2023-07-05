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

import java.nio.file.Path;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class DatasetXmlContainsAtMostOneIdentifierWithIdTypeDoi implements BagValidatorRule {
    private XmlReader xmlReader;

    @Override
    public RuleResult validate(Path path) throws Exception {
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
        var idTypePrefix = document.lookupPrefix(XmlReader.NAMESPACE_ID_TYPE);
        var expr = String.format("/ddm:DDM/ddm:dcmiMetadata/dcterms:identifier[@xsi:type=\"%s:DOI\"]", idTypePrefix);
        var count = xmlReader.xpathToStreamOfStrings(document, expr).count();

        if (count == 0) {
            return RuleResult.skipDependencies();
        }
        else if (count == 1) {
            return RuleResult.ok();
        }
        else {
            return RuleResult.error(String.format(
                "dataset.xml: More than one identifier with xsi:type=\"%s:DOI\" found", idTypePrefix
            ));
        }
    }
}
