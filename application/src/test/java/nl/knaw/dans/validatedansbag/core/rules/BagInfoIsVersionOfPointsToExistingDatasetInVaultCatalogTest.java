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
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.VaultService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BagInfoIsVersionOfPointsToExistingDatasetInVaultCatalogTest {

    @Test
    void validate_should_return_SUCCESS_when_VaultService_yields_items() throws Exception {
        var vaultService = Mockito.mock(VaultService.class);
        var metadataReader = Mockito.mock(BagItMetadataReader.class);

        Mockito.doReturn("urn:uuid:is-version-of-id")
            .when(metadataReader).getSingleField(Mockito.any(), Mockito.eq("Is-Version-Of"));

        Mockito.doReturn(Optional.of(new VaultService.VaultEntry("urn:uuid:is-version-of-id")))
            .when(vaultService).findDatasetBySwordToken(Mockito.eq("urn:uuid:is-version-of-id"));

        var rule = new BagInfoIsVersionOfPointsToExistingDatasetInVaultCatalog(vaultService, metadataReader);
        var result = rule.validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void validate_should_return_SKIP_DEPENDENCIES_when_swordToken_is_not_there() throws Exception {
        var vaultService = Mockito.mock(VaultService.class);
        var metadataReader = Mockito.mock(BagItMetadataReader.class);
        var rule = new BagInfoIsVersionOfPointsToExistingDatasetInVaultCatalog(vaultService, metadataReader);
        var result = rule.validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SKIP_DEPENDENCIES, result.getStatus());
    }

    @Test
    void validate_should_return_ERROR_when_VaultService_yields_nothing() throws Exception {
        var vaultService = Mockito.mock(VaultService.class);
        var metadataReader = Mockito.mock(BagItMetadataReader.class);

        Mockito.doReturn(Optional.empty())
            .when(vaultService).findDatasetBySwordToken(Mockito.eq("urn:uuid:is-version-of-id"));

        Mockito.doReturn("urn:uuid:is-version-of-id")
            .when(metadataReader).getSingleField(Mockito.any(), Mockito.eq("Is-Version-Of"));

        var rule = new BagInfoIsVersionOfPointsToExistingDatasetInVaultCatalog(vaultService, metadataReader);
        var result = rule.validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void validate_should_throw_exception_if_called_without_VaultService() throws Exception {
        var metadataReader = Mockito.mock(BagItMetadataReader.class);
        var rule = new BagInfoIsVersionOfPointsToExistingDatasetInVaultCatalog(null, metadataReader);

        assertThrows(IllegalStateException.class, () -> rule.validate(Path.of("bagdir")));
    }
}