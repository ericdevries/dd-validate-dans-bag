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

public class BagInfoOrganizationalIdentifierExistsInDatasetTest extends RuleTestFixture {

    @Test
    void should_return_SUCCESS_if_otherId_matches_hasOrganizationalIdentifier() throws Exception {
        var isVersionOf = "urn:uuid:some-uuid";
        var otherId = "other-id";
        var hasOrganizationalIdentifier = "other-id";

        Mockito.doReturn(isVersionOf)
                .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.eq("Is-Version-Of"));

        Mockito.doReturn(hasOrganizationalIdentifier)
                .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.eq("Has-Organizational-Identifier"));

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, otherId));

        var result = new BagInfoOrganizationalIdentifierExistsInDataset(dataverseService, bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_SUCCESS_if_both_values_are_null() throws Exception {
        Mockito.doReturn("urn:uuid:some-uuid")
                .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.eq("Is-Version-Of"));

        Mockito.doReturn(null)
                .when(bagItMetadataReader).getSingleField(Mockito.any(), Mockito.eq("Has-Organizational-Identifier"));

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, null));

        var result = new BagInfoOrganizationalIdentifierExistsInDataset(dataverseService, bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_ERROR_when_dataset_is_null_and_metadata_is_not_null() throws Exception {
        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.anyString()))
                .thenReturn("urn:uuid:is_version_of")
                .thenReturn("has_organizational_identifier");

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, null));

        var result = new BagInfoOrganizationalIdentifierExistsInDataset(dataverseService, bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_ERROR_if_values_do_not_match() throws Exception {
        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.anyString()))
                .thenReturn("urn:uuid:is_version_of")
                .thenReturn("has_organizational_identifier");

        var doi = "doi:10.5072/FK2/QZZSST";
        mockSearchBySwordToken(getSearchResult(doi));
        mockGetDataset(getLatestVersion(doi, "some_other_organizational_identifier"));

        var result = new BagInfoOrganizationalIdentifierExistsInDataset(dataverseService, bagItMetadataReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

}
