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
package nl.knaw.dans.validatedansbag.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.HttpClientConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Valid
public class ValidationConfig {

    @NotNull
    @Valid
    private List<String> otherIdPrefixes;

    @Valid
    @NotNull
    private XmlSchemaConfig xmlSchemas;

    @Valid
    private HttpClientConfiguration httpClient = new HttpClientConfiguration();

    public List<String> getOtherIdPrefixes() {
        return otherIdPrefixes;
    }

    public void setOtherIdPrefixes(List<String> otherIdPrefixes) {
        this.otherIdPrefixes = otherIdPrefixes;
    }

    public XmlSchemaConfig getXmlSchemas() {
        return xmlSchemas;
    }

    public void setXmlSchemas(XmlSchemaConfig xmlSchemas) {
        this.xmlSchemas = xmlSchemas;
    }

    public HttpClientConfiguration getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClientConfiguration httpClient) {
        this.httpClient = httpClient;
    }
}
