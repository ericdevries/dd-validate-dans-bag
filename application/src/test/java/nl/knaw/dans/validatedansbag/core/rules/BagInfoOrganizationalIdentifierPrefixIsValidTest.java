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
import nl.knaw.dans.validatedansbag.core.validator.OrganizationIdentifierPrefixValidator;
import nl.knaw.dans.validatedansbag.core.validator.OrganizationIdentifierPrefixValidatorImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BagInfoOrganizationalIdentifierPrefixIsValidTest extends RuleTestFixture {
    private final OrganizationIdentifierPrefixValidator organizationIdentifierPrefixValidator = new OrganizationIdentifierPrefixValidatorImpl(
            List.of("USER1-", "U2:")
    );

    @Test
    void should_return_SUCCESS_if_prefix_is_on_configured_list() throws Exception {
        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.any()))
                .thenReturn("USER1-organizational-identifier");

        var result = new BagInfoOrganizationalIdentifierPrefixIsValid(bagItMetadataReader, organizationIdentifierPrefixValidator).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_ERROR_if_prefix_is_not_on_configured_list() throws Exception {
        Mockito.when(bagItMetadataReader.getSingleField(Mockito.any(), Mockito.any()))
                .thenReturn("WRONG-organizational-identifier");

        var result = new BagInfoOrganizationalIdentifierPrefixIsValid(bagItMetadataReader, organizationIdentifierPrefixValidator).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }
}
