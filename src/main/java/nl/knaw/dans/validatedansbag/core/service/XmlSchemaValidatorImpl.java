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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlSchemaValidatorImpl implements XmlSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(XmlSchemaValidatorImpl.class);
    protected final Map<String, String> schemaUrls = Map.of(
        "ddm", "https://easy.dans.knaw.nl/schemas/md/ddm/ddm.xsd",
        "files", "https://easy.dans.knaw.nl/schemas/bag/metadata/files/files.xsd",
        "agreements", "https://easy.dans.knaw.nl/schemas/bag/metadata/agreements/agreements.xsd",
        "provenance", "https://easy.dans.knaw.nl/schemas/bag/metadata/prov/provenance.xsd",
        "amd", "https://easy.dans.knaw.nl/schemas/bag/metadata/amd/amd.xsd",
        "emd", "https://easy.dans.knaw.nl/schemas/md/emd/emd.xsd"
    );

    protected final Map<String, String> filenameToSchemaMap = Map.of(
        "dataset.xml", "ddm",
        "files.xml", "files",
        "agreements.xml", "agreements",
        "provenance.xml", "provenance",
        "amd.xml", "amd",
        "emd.xml", "emd"
    );

    protected final Map<String, Schema> validators = new HashMap<>();
    private final SchemaFactory schemaFactory;

    public XmlSchemaValidatorImpl() {
        this.schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    }

    Schema getValidatorForFilename(String filename) throws MalformedURLException, SAXException {
        var result = validators.get(filename);

        if (result == null) {
            var schema = filenameToSchemaMap.get(filename);

            if (schema != null) {
                result = schemaFactory.newSchema(new URL(schemaUrls.get(schema)));
            }
            else {
                log.warn("Requested XML schema for filename {} but this filename is unknown", filename);
                return null;
            }
        }

        return result;
    }

    @Override
    public List<SAXParseException> validateDocument(Node node, String schema) throws IOException, SAXException {
        var schemaInstance = getValidatorForFilename(schema);

        if (schemaInstance == null) {
            throw new NullPointerException(String.format("No validator found for key %s", schema));
        }

        var validator = schemaInstance.newValidator();
        var exceptions = new ArrayList<SAXParseException>();

        validator.setErrorHandler(new ErrorHandler() {

            // TODO verify that a warning should also result in an error
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
}
