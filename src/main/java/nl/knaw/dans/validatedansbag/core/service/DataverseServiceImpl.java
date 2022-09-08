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
import nl.knaw.dans.lib.dataverse.DataverseHttpResponse;
import nl.knaw.dans.lib.dataverse.DataverseResponse;
import nl.knaw.dans.lib.dataverse.SearchOptions;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetLatestVersion;
import nl.knaw.dans.lib.dataverse.model.search.SearchItemType;
import nl.knaw.dans.lib.dataverse.model.search.SearchResult;
import nl.knaw.dans.validatedansbag.core.config.DataverseConfig;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

public class DataverseServiceImpl implements DataverseService {
    private final DataverseConfig dataverseConfig;
    private DataverseClient dataverseClient;

    public DataverseServiceImpl(DataverseConfig dataverseConfig) {
        this.dataverseConfig = dataverseConfig;
    }

    public DataverseClient getDataverseClient() {
        if (this.dataverseClient == null) {
            var config = new DataverseClientConfig(URI.create(dataverseConfig.getBaseUrl()), dataverseConfig.getApiToken());
            this.dataverseClient = new DataverseClient(config);
        }

        return dataverseClient;
    }

    @Override
    public DataverseResponse<SearchResult> searchBySwordToken(String token) throws IOException, DataverseException {
        var client = this.getDataverseClient();

        return client.search().find(String.format("dansSwordToken:%s", token));
    }

    @Override
    public DataverseResponse<SearchResult> searchDatasetsByOrganizationalIdentifier(String identifier) throws IOException, DataverseException {
        var client = this.getDataverseClient();
        var options = new SearchOptions();
        options.setTypes(List.of(SearchItemType.dataset));

        return client.search().find(String.format("dansOtherId:%s", identifier), options);
    }

    @Override
    public DataverseHttpResponse<List<RoleAssignmentReadOnly>> getRoleAssignments(String identifier) throws IOException, DataverseException {
        var client = this.getDataverseClient();
        return client.dataset(identifier).listRoleAssignments();

    }

    @Override
    public DataverseResponse<DatasetLatestVersion> getDataset(String globalId) throws IOException, DataverseException {
        var client = this.getDataverseClient();

        return client.dataset(globalId).getLatestVersion();
    }

    @Override
    public Set<String> getAllowedDepositorRoles() {
        return dataverseConfig.getAllowedDepositorRoles();
    }
}
