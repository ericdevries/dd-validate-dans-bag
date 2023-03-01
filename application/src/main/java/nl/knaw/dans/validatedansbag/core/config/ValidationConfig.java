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
import java.net.URL;
import java.util.List;

@Valid
public class ValidationConfig {

    @NotNull
    @Valid
    private SwordDepositorRoles swordDepositorRoles;

    @NotNull
    @Valid
    private List<OtherIdPrefix> otherIdPrefixes;

    @Valid
    @NotNull
    private XmlSchemaConfig xmlSchemas;

    @Valid
    private URL passwordDelegate;
    @Valid
    private String passwordRealm;

    @Valid
    private HttpClientConfiguration httpClient = new HttpClientConfiguration();

    public SwordDepositorRoles getSwordDepositorRoles() {
        return swordDepositorRoles;
    }

    public void setSwordDepositorRoles(SwordDepositorRoles swordDepositorRoles) {
        this.swordDepositorRoles = swordDepositorRoles;
    }

    public List<OtherIdPrefix> getOtherIdPrefixes() {
        return otherIdPrefixes;
    }

    public void setOtherIdPrefixes(List<OtherIdPrefix> otherIdPrefixes) {
        this.otherIdPrefixes = otherIdPrefixes;
    }

    public XmlSchemaConfig getXmlSchemas() {
        return xmlSchemas;
    }

    public void setXmlSchemas(XmlSchemaConfig xmlSchemas) {
        this.xmlSchemas = xmlSchemas;
    }

    public URL getPasswordDelegate() {
        return passwordDelegate;
    }

    public void setPasswordDelegate(URL passwordDelegate) {
        this.passwordDelegate = passwordDelegate;
    }

    public String getPasswordRealm() {
        return passwordRealm;
    }

    public void setPasswordRealm(String passwordRealm) {
        this.passwordRealm = passwordRealm;
    }

    @JsonProperty("httpClient")
    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClient;
    }

    @JsonProperty("httpClient")
    public void setHttpClientConfiguration(HttpClientConfiguration httpClient) {
        this.httpClient = httpClient;
    }
}
