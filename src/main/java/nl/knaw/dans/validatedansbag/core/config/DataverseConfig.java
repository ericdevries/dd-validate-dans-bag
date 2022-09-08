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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
public class DataverseConfig {
    @Valid
    @NotNull
    private String baseUrl;
    @Valid
    @NotNull
    private String apiToken;
    @NotNull
    private SwordDepositorRoles swordDepositorRoles;

    public DataverseConfig() {

    }

    public DataverseConfig(String baseUrl, String apiToken, SwordDepositorRoles swordDepositorRoles) {
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;
        this.swordDepositorRoles = swordDepositorRoles;
    }

    public SwordDepositorRoles getSwordDepositorRoles() {
        return swordDepositorRoles;
    }

    public void setSwordDepositorRoles(SwordDepositorRoles swordDepositorRoles) {
        this.swordDepositorRoles = swordDepositorRoles;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
