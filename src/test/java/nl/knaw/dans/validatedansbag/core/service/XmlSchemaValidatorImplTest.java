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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XmlSchemaValidatorImplTest {

    Node readFile(InputStream is) throws ParserConfigurationException, IOException, SAXException {

        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(true);

        return factory
            .newDocumentBuilder()
            .parse(is);
    }

    XmlSchemaValidator getValidator() throws MalformedURLException, SAXException {
        var validator = Mockito.spy(new XmlSchemaValidatorImpl());
        var schemaMapping = Map.of(
            "dataset.xml", "ddm",
            "files.xml", "files",
            "agreements.xml", "agreements",
            "provenance.xml", "provenance",
            "amd.xml", "amd",
            "emd.xml", "emd"
        );

        Mockito.doAnswer(new Answer<Schema>() {

            @Override
            public Schema answer(InvocationOnMock invocationOnMock) throws Throwable {
                var path = schemaMapping.get(invocationOnMock.getArgument(0, String.class)) + ".xsd";
                try (var resource = getClass().getClassLoader().getResourceAsStream("xml/schemas/" + path)) {

                    if (resource == null) {
                        return null;
                    }

                    var schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
                    return schemaFactory.newSchema(new StreamSource(resource));
                }
            }
        }).when(validator).getValidatorForFilename(Mockito.anyString());

        return validator;
    }

    @Test
    void testValidateDocument() throws Exception {
        var doc = readFile(getClass().getClassLoader().getResourceAsStream("xml/testfiles/valid-dataset.xml"));
        var validator = getValidator();

        var result = validator.validateDocument(doc, "dataset.xml");
        assertEquals(0, result.size());
    }

    @Test
    void testValidateInvalidDocument() throws Exception {
        var doc = readFile(getClass().getClassLoader().getResourceAsStream("xml/testfiles/invalid-dataset.xml"));
        var validator = getValidator();

        var result = validator.validateDocument(doc, "dataset.xml");
        assertEquals(1, result.size());
    }

    @Test
    void testNonExistingSchema() throws Exception {
        var doc = readFile(getClass().getClassLoader().getResourceAsStream("xml/testfiles/invalid-dataset.xml"));
        var validator = getValidator();

        assertThrows(NullPointerException.class, () -> validator.validateDocument(doc, "unknown.xml"));
    }

}