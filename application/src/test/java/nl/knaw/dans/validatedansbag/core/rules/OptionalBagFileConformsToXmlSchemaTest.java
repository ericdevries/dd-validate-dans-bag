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
import org.xml.sax.SAXParseException;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OptionalBagFileConformsToXmlSchemaTest extends RuleTestFixture {

    @Test
    void should_return_ERROR_status_if_file_exists_but_does_not_validate() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<ddm:DDM xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\" xmlns=\"http://easy.dans.knaw.nl/schemas/bag/metadata/files/\" xmlns:abr=\"http://www.den.nl/standaard/166/Archeologisch-Basisregister/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\" xmlns:dcx-gml=\"http://easy.dans.knaw.nl/schemas/dcx/gml/\" xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                + "    <ddm:profile>\n"
                + "        <dcterms:description xml:lang=\"en\">This find is registered at Portable Antiquities of the Netherlands with number PAN-00008136</dcterms:description>\n"
                + "    </ddm:profile>\n"
                + "</ddm:DDM>\n";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(true).when(fileService).exists(Path.of("bagdir/metadata/dataset.xml"));
        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());
        Mockito.doReturn(List.of(new SAXParseException("msg", null)))
                .when(xmlSchemaValidator).validateDocument(Mockito.any(), Mockito.anyString());

        var result = new OptionalBagFileConformsToXmlSchema(Path.of("metadata/dataset.xml"), reader, "ddm", xmlSchemaValidator, fileService).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_SKIP_DEPENDENCIES_status_if_file_does_not_exist() throws Exception {
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(false).when(fileService).exists(Path.of("bagdir/metadata/dataset.xml"));
        var result = new OptionalBagFileConformsToXmlSchema(Path.of("metadata/dataset.xml"), reader, "ddm", xmlSchemaValidator, fileService).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SKIP_DEPENDENCIES, result.getStatus());

    }
}
