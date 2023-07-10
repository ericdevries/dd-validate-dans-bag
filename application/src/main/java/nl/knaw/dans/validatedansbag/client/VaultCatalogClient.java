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
package nl.knaw.dans.validatedansbag.client;

import nl.knaw.dans.validatedansbag.core.service.VaultService;

import java.io.IOException;
import java.util.Optional;

public class VaultCatalogClient implements VaultService {
    private final OcflObjectVersionApi ocflObjectVersionApi;

    public VaultCatalogClient(OcflObjectVersionApi ocflObjectVersionApi) {
        this.ocflObjectVersionApi = ocflObjectVersionApi;
    }

    @Override
    public Optional<VaultEntry> findDatasetBySwordToken(String swordToken) throws IOException {
        try {
            var vaultEntry = ocflObjectVersionApi.getOcflObjectsBySwordToken(swordToken);

            return vaultEntry.stream()
                .map(item -> new VaultEntry(item.getSwordToken()))
                .findFirst();

        }
        catch (Exception e) {
            throw new IOException("Unable to fetch vault entry", e);
        }
    }
}
