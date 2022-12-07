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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlSchemaValidatorImpl implements XmlSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(XmlSchemaValidatorImpl.class);

    private final Map<String, URI> filenameToSchemaLocation;

    protected final Map<String, Schema> filenameToSchemaInstance = new HashMap<>();
    private final SchemaFactory schemaFactory;

    public XmlSchemaValidatorImpl(Map<String, URI> filenameToSchemaLocation) {
        this.filenameToSchemaLocation = filenameToSchemaLocation;
        this.schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        try {
            this.loadSchemaInstances();
        }
        catch (Throwable e) {
            log.error("Unable to load XML schema's on startup", e);
        }
    }

    @Override
    public List<SAXParseException> validateDocument(Node node, String schema) throws IOException, SAXException {
        var schemaInstance = getSchemaInstanceFor(schema);

        if (schemaInstance == null) {
            throw new IllegalStateException(String.format("No schema instance found for key %s", schema));
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

    @Override
    public void loadSchemaInstances() {
        for (var filename : filenameToSchemaLocation.keySet()) {
            log.trace("Start loading of schema instance for {}", filename);
            if (filenameToSchemaInstance.get(filename) != null) {
                log.trace("Schema instance {} already loaded, skipping", filename);
                continue;
            }

            try {
                log.info("Loading validator for {}...", filename);
                getSchemaInstanceFor(filename);
                log.info("Validator for {} loaded.", filename);
            }
            catch (MalformedURLException | SAXException e) {
                log.error("Unable to load validator for filename {}", filename, e);
                var url = filenameToSchemaLocation.get(filename);
                throw new RuntimeException(String.format("Unable to load XSD '%s'", url), e);
            }
        }
    }

    private Schema getSchemaInstanceFor(String filename) throws MalformedURLException, SAXException {
        log.debug("Looking up validator schema for file {}", filename);
        var schemaInstance = filenameToSchemaInstance.get(filename);
        log.debug("Found validator schema {}", schemaInstance);

        if (schemaInstance == null) {
            log.debug("Schema instance not yet loaded. Looking for schema location...");
            var schemaLocation = filenameToSchemaLocation.get(filename);

            if (schemaLocation != null) {
                log.debug("Found schema location: {}", schemaLocation);
                schemaInstance = schemaFactory.newSchema(new URL(schemaLocation.toASCIIString()));
                log.debug("Caching schema instance for {}", schemaLocation);
                filenameToSchemaInstance.put(filename, schemaInstance);
            }
            else {
                throw new IllegalStateException(String.format("Requested XML schema for filename %s but not schema location is configured for this filename", filename));
            }
        }

        return schemaInstance;
    }
}
