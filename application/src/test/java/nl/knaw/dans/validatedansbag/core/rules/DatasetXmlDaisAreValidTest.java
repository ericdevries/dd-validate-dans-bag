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

import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetXmlDaisAreValidTest extends RuleTestFixture {
    @Test
    void should_return_SUCCESS_when_no_invalid_dais_found() throws Exception {
        final String xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:profile>\n"
                + "        <dcx-dai:creatorDetails>\n"
                + "            <dcx-dai:author>\n"
                + "                <dcx-dai:DAI>123456789</dcx-dai:DAI><!-- <=== VALID DAI -->\n"
                + "                <dcx-dai:organization>\n"
                + "                    <dcx-dai:DAI>info:eu-repo/dai/nl/298814064</dcx-dai:DAI><!-- <=== VALID DAI -->\n"
                + "                </dcx-dai:organization>\n"
                + "                <dcx-dai:ISNI>http://www.isni.org/isni/0000000114559647</dcx-dai:ISNI>\n"
                + "                <dcx-dai:ORCID>http://orcid.org/0000-0002-1825-0097</dcx-dai:ORCID>\n"
                + "            </dcx-dai:author>\n"
                + "        </dcx-dai:creatorDetails>\n"
                + "    </ddm:profile>\n"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        assertEquals(RuleResult.Status.SUCCESS, new DatasetXmlDaisAreValid(reader, identifierValidator).validate(Path.of("bagdir")).getStatus());
    }

    @Test
    void should_return_ERRO_when_only_invalid_dais_found() throws Exception {
        final String xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:profile>\n"
                + "        <dcx-dai:creatorDetails>\n"
                + "            <dcx-dai:author>\n"
                + "                <dcx-dai:DAI>123456788</dcx-dai:DAI><!-- <=== INVALID DAI -->\n"
                + "                <dcx-dai:organization>\n"
                + "                    <dcx-dai:DAI>info:eu-repo/dai/nl/298814063</dcx-dai:DAI><!-- <=== INVALID DAI -->\n"
                + "                </dcx-dai:organization>\n"
                + "            </dcx-dai:author>\n"
                + "        </dcx-dai:creatorDetails>\n"
                + "    </ddm:profile>\n"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlDaisAreValid(reader, identifierValidator).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

}
