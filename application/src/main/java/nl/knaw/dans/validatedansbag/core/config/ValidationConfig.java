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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Valid
public class ValidationConfig {

    @NotNull
    @JsonProperty("licenses")
    private LicenseConfig licenseConfig;
    @NotNull
    private SwordDepositorRoles swordDepositorRoles;
    @NotNull
    private List<OtherIdPrefix> otherIdPrefixes;

    @NotNull
    private XmlSchemaConfig xmlSchemas;

    public LicenseConfig getLicenseConfig() {
        return licenseConfig;
    }

    public void setLicenseConfig(LicenseConfig licenseConfig) {
        this.licenseConfig = licenseConfig;
    }

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
}
