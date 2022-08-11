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

public class BagXmlReaderImpl implements BagXmlReader {
    private final String namespaceDc = "http://purl.org/dc/elements/1.1/";
    private final String namespaceDcxDai = "http://easy.dans.knaw.nl/schemas/dcx/dai/";
    private final String namespaceDdm = "http://easy.dans.knaw.nl/schemas/md/ddm/";
    private final String namespaceDcterms = "http://purl.org/dc/terms/";
    private final String namespaceXsi = "http://www.w3.org/2001/XMLSchema-instance";
    private final String namespaceIdType = "http://easy.dans.knaw.nl/schemas/vocab/identifier-type/";

    private final String namespaceDcxGml = "http://easy.dans.knaw.nl/schemas/dcx/gml/";

    private final String namespaceFilesXml = "http://easy.dans.knaw.nl/schemas/bag/metadata/files/";

    private final String namespaceOpenGis = "http://www.opengis.net/gml";

    private final XPath xpath;

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
            "dcx-gml", namespaceDcxGml,
            "files", namespaceFilesXml,
            "gml", namespaceOpenGis
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
    public Stream<Node> xpathToStream(Node node, String expression) throws XPathExpressionException {
        var nodes = (NodeList) evaluateXpath(node, expression, XPathConstants.NODESET);

        return IntStream.range(0, nodes.getLength())
            .mapToObj(nodes::item);
    }

    @Override
    public Stream<Node> xpathsToStream(Node node, Collection<String> expressions) throws XPathExpressionException {
        var items = new ArrayList<Stream<Node>>();

        for (var expr: expressions) {
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
