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

import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BagInfoIsVersionOfPointsToExistingDatasetInDataverseTest extends RuleTestFixture {
    @Test
    void should_return_SUCCESS_if_bag_exists() throws Exception {
        Mockito.doReturn("urn:uuid:is-version-of-id")
                .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, null));

        var result = new BagInfoIsVersionOfPointsToExistingDatasetInDataverse(dataverseService, bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_ERROR_when_search_yields_zero_results() throws Exception {
        Mockito.doReturn("urn:uuid:is-version-of-id")
                .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.anyString());

        String emptySearchResult = getEmptySearchResult();
        mockSearchBySwordToken(emptySearchResult);

        var result = new BagInfoIsVersionOfPointsToExistingDatasetInDataverse(dataverseService, bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }
}
