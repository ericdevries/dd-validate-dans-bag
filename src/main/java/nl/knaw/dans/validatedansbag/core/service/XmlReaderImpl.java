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
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class XmlReaderImpl implements XmlReader {

    private final XPath xpath;

    public XmlReaderImpl() {
        this.xpath = XPathFactory
            .newInstance()
            .newXPath();

        final var namespaceMap = Map.of(
            "dc", NAMESPACE_DC,
            "dcx-dai", NAMESPACE_DCX_DAI,
            "ddm", NAMESPACE_DDM,
            "dcterms", NAMESPACE_DCTERMS,
            "xsi", NAMESPACE_XSI,
            "id-type", NAMESPACE_ID_TYPE,
            "dcx-gml", NAMESPACE_DCX_GML,
            "files", NAMESPACE_FILES_XML,
            "gml", NAMESPACE_OPEN_GIS
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

    public Document readXmlString(String str) throws ParserConfigurationException, IOException, SAXException {
        var factory = getFactory();

        return factory
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(str)));
    }

    private Object evaluateXpath(Node node, String expr, QName type) throws XPathExpressionException {
        return xpath.compile(expr).evaluate(node, type);
    }

    @Override
    public Stream<Node> xpathToStream(Node node, String expression) throws XPathExpressionException {
        var nodes = (NodeList) evaluateXpath(node, expression, XPathConstants.NODESET);

        return IntStream.range(0, nodes.getLength())
            .mapToObj(nodes::item);
    }

    @Override
    public Stream<Node> xpathsToStream(Node node, Collection<String> expressions) throws XPathExpressionException {
        var items = new ArrayList<Stream<Node>>();

        for (var expr : expressions) {
            var item = xpathToStream(node, expr);
            items.add(item);
        }

        return items.stream().flatMap(i -> i);
    }

    private DocumentBuilderFactory getFactory() throws ParserConfigurationException {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(true);
        return factory;
    }
}
