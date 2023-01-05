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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;

public interface XmlReader {

    String NAMESPACE_DC = "http://purl.org/dc/elements/1.1/";
    String NAMESPACE_DCX_DAI = "http://easy.dans.knaw.nl/schemas/dcx/dai/";
    String NAMESPACE_DDM = "http://schemas.dans.knaw.nl/dataset/ddm-v2/";
    String NAMESPACE_DCTERMS = "http://purl.org/dc/terms/";
    String NAMESPACE_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    String NAMESPACE_ID_TYPE = "http://easy.dans.knaw.nl/schemas/vocab/identifier-type/";
    String NAMESPACE_DCX_GML = "http://easy.dans.knaw.nl/schemas/dcx/gml/";
    String NAMESPACE_FILES_XML = "http://easy.dans.knaw.nl/schemas/bag/metadata/files/";
    String NAMESPACE_OPEN_GIS = "http://www.opengis.net/gml";

    Document readXmlFile(Path path) throws ParserConfigurationException, IOException, SAXException;

    Stream<Node> xpathToStream(Node node, String expression) throws XPathExpressionException;

    Stream<Node> xpathsToStream(Node node, Collection<String> expressions) throws XPathExpressionException;

    Stream<String> xpathToStreamOfStrings(Node node, String expression) throws XPathExpressionException;

    Stream<String> xpathsToStreamOfStrings(Node node, Collection<String> expressions) throws XPathExpressionException;
}
