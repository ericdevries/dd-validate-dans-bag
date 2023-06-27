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
package nl.knaw.dans.validatedansbag.core.rules;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetLatestVersion;
import nl.knaw.dans.lib.dataverse.model.search.DatasetResultItem;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;

import java.io.IOException;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
public class DataverseRuleBase {
    protected final DataverseService dataverseService;

    protected Optional<DatasetLatestVersion> getDatasetIsVersionOf(String isVersionOf) throws IOException, DataverseException {
        if (isVersionOf.startsWith("urn:uuid:")) {
            var swordToken = "sword:" + isVersionOf.substring("urn:uuid:".length());
            var result = dataverseService.searchBySwordToken(swordToken)
                    .getData().getItems().stream()
                    .filter(resultItem -> resultItem instanceof DatasetResultItem)
                    .map(resultItem -> (DatasetResultItem) resultItem)
                    .findFirst();
            if (result.isPresent()) {
                var globalId = result.get().getGlobalId();
                return Optional.ofNullable(dataverseService.getDataset(globalId).getData());
            }

            return Optional.empty();
        } else {
            throw new IllegalArgumentException("Is-Version-Of is not a urn:uuid");
        }
    }
}
