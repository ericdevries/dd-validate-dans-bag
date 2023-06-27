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
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
public class DatasetXmlHasRightsHolderRuleBase {
    protected final XmlReader xmlReader;

    protected Optional<String> getRightsHolderInElement(Document document) throws XPathExpressionException {
        return xmlReader.xpathToStreamOfStrings(document, "/ddm:DDM/ddm:dcmiMetadata//dcterms:rightsHolder")
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst();
    }

    protected Optional<String> getRightsHolderInAuthor(Document document) throws XPathExpressionException {
        return xmlReader.xpathToStreamOfStrings(document, "//dcx-dai:author/dcx-dai:role")
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> value.equals("RightsHolder"))
                .findFirst();
    }
}
