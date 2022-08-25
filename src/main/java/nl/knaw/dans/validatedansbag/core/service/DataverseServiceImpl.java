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

import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseClientConfig;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.search.SearchResult;
import nl.knaw.dans.validatedansbag.core.config.DataverseConfig;

import java.io.IOException;
import java.net.URI;

public class DataverseServiceImpl implements DataverseService {
    private DataverseClient dataverseClient;
    private final DataverseConfig dataverseConfig;

    public DataverseServiceImpl(DataverseConfig dataverseConfig) {
        this.dataverseConfig = dataverseConfig;
    }

    DataverseClient getDataverseClient() {
        if (this.dataverseClient == null) {
            var config = new DataverseClientConfig(URI.create(dataverseConfig.getBaseUrl()), dataverseConfig.getApiToken());
            this.dataverseClient = new DataverseClient(config);
        }

        return dataverseClient;
    }
    @Override
    public SearchResult searchBySwordToken(String token) throws IOException, DataverseException {
        var client = this.getDataverseClient();
        var response = client.search().find(String.format("dansDataVaultMetadata:%s", token));

        return response.getData();
    }
}
