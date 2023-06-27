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
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class XmlSchemaConfig {

    @NotNull
    @JsonProperty("dataset.xml")
    private URI datasetXml;
    @NotNull
    @JsonProperty("files.xml")
    private URI filesXml;

    @NotNull
    @JsonProperty("agreements.xml")
    private URI agreementsXml;

    @NotNull
    @JsonProperty("provenance.xml")
    private URI provenanceXml;

    @NotNull
    @JsonProperty("amd.xml")
    private URI amdXml;

    @NotNull
    @JsonProperty("emd.xml")
    private URI emdXml;

    public Map<String, URI> buildMap() {
        var map = new HashMap<String, URI>();
        map.put("dataset.xml", datasetXml);
        map.put("files.xml", filesXml);
        map.put("agreements.xml", agreementsXml);
        map.put("provenance.xml", provenanceXml);
        map.put("amd.xml", amdXml);
        map.put("emd.xml", emdXml);
        return map;
    }
}
