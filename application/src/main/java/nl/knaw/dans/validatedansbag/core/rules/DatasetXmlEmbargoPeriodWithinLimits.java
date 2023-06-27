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
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.stream.Collectors;

@Slf4j
public class DatasetXmlEmbargoPeriodWithinLimits extends DataverseRuleBase implements BagValidatorRule {
    private final XmlReader xmlReader;

    public DatasetXmlEmbargoPeriodWithinLimits(DataverseService dataverseService, XmlReader xmlReader) {
        super(dataverseService);
        this.xmlReader = xmlReader;
    }

    @Override
    public RuleResult validate(Path path) throws Exception {
        var months = Integer.parseInt(dataverseService.getMaxEmbargoDurationInMonths().getData().getMessage());
        var document = xmlReader.readXmlFile(path.resolve("metadata/dataset.xml"));
        var expr = "/ddm:DDM/ddm:profile/ddm:available";

        var nodes = xmlReader.xpathToStream(document, expr).collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return RuleResult.ok();
        }

        DateTime embargoDate = DateTime.parse(nodes.get(0).getTextContent());
        if (embargoDate.isBefore(new DateTime(DateTime.now().plusMonths(months)))) {
            return RuleResult.ok();
        } else {
            return RuleResult.error("Date available is further in the future than the Embargo Period allows");
        }
    }
}
