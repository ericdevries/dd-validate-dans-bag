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

import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.DataverseResponse;
import nl.knaw.dans.lib.dataverse.model.DataMessage;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetLatestVersion;
import nl.knaw.dans.lib.dataverse.model.license.License;
import nl.knaw.dans.lib.dataverse.model.search.SearchResult;

import java.io.IOException;
import java.util.List;

public interface DataverseService {

    DataverseResponse<SearchResult> searchBySwordToken(String token) throws IOException, DataverseException;

    DataverseResponse<SearchResult> searchDatasetsByOrganizationalIdentifier(String identifier) throws IOException, DataverseException;

    DataverseResponse<List<RoleAssignmentReadOnly>> getDatasetRoleAssignments(String identifier) throws IOException, DataverseException;

    DataverseResponse<DatasetLatestVersion> getDataset(String globalId) throws IOException, DataverseException;

    DataverseResponse<List<RoleAssignmentReadOnly>> getDataverseRoleAssignments(String itemId) throws IOException, DataverseException;

    DataverseResponse<DataMessage> getMaxEmbargoDurationInMonths() throws IOException, DataverseException;

    List<License> getLicenses() throws IOException, DataverseException;

    void checkConnection() throws IOException, DataverseException;
}
