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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BagXmlReaderImpl implements BagXmlReader {
    private final String namespaceDc = "http://purl.org/dc/elements/1.1/";
    private final String namespaceDcxDai = "http://easy.dans.knaw.nl/schemas/dcx/dai/";
    private final String namespaceDdm = "http://easy.dans.knaw.nl/schemas/md/ddm/";
    private final String namespaceDcterms = "http://purl.org/dc/terms/";
    private final String namespaceXsi = "http://www.w3.org/2001/XMLSchema-instance";
    private final String namespaceIdType = "http://easy.dans.knaw.nl/schemas/vocab/identifier-type/";

    private final String namespaceDcxGml = "http://easy.dans.knaw.nl/schemas/dcx/gml/";

    private final XPath xpath;

    // TODO make configurable?
    private final Map<String, String> schemaUrls = Map.of(
        "ddm", "https://easy.dans.knaw.nl/schemas/md/ddm/ddm.xsd",
        "files", "https://easy.dans.knaw.nl/schemas/bag/metadata/files/files.xsd",
        "agreements", "https://easy.dans.knaw.nl/schemas/bag/metadata/agreements/agreements.xsd",
        "provenance", "https://easy.dans.knaw.nl/schemas/bag/metadata/prov/provenance.xsd",
        "amd", "https://easy.dans.knaw.nl/schemas/bag/metadata/amd/amd.xsd",
        "emd", "https://easy.dans.knaw.nl/schemas/md/emd/emd.xsd"
    );

    public BagXmlReaderImpl() {
        this.xpath = XPathFactory
            .newInstance()
            .newXPath();

        final var namespaceMap = Map.of(
            "dc", namespaceDc,
            "dcx-dai", namespaceDcxDai,
            "ddm", namespaceDdm,
            "dcterms", namespaceDcterms,
            "xsi", namespaceXsi,
            "id-type", namespaceIdType,
            "dcx-gml", namespaceDcxGml
        );

        xpath.setNamespaceContext(new NamespaceContext() {

            @Override
            public String getNamespaceURI(String s) {
                return namespaceMap.get(s);
            }

            @Override
            public String getPrefix(String s) {
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String s) {
                return null;
            }
        });

    }

    @Override
    public Document readXmlFile(Path path) throws ParserConfigurationException, IOException, SAXException {
        var factory = getFactory();

        return factory
            .newDocumentBuilder()
            .parse(path.toFile());
    }

    @Override
    public Document readXmlString(String str) throws ParserConfigurationException, IOException, SAXException {
        var factory = getFactory();

        return factory
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(str)));
    }

    @Override
    public Object evaluateXpath(Node node, String expr, QName type) throws XPathExpressionException {
        return xpath.compile(expr).evaluate(node, type);
    }

    @Override
    public List<SAXParseException> validateXmlWithSchema(Node node, String schema) throws IOException, SAXException {
        var schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        var s = schemaFactory.newSchema(new URL(schemaUrls.get(schema)));
        // TODO cache the schema creation thing
        var validator = s.newValidator();
        var exceptions = new ArrayList<SAXParseException>();

        validator.setErrorHandler(new ErrorHandler() {

            @Override
            public void warning(SAXParseException e) {
                exceptions.add(e);
            }

            @Override
            public void error(SAXParseException e) {
                exceptions.add(e);
            }

            @Override
            public void fatalError(SAXParseException e) {
                exceptions.add(e);
            }
        });

        validator.validate(new DOMSource(node));

        return exceptions;
    }

    private DocumentBuilderFactory getFactory() throws ParserConfigurationException {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(true);
        return factory;
    }
}
