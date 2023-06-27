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

import nl.knaw.dans.validatedansbag.core.engine.RuleEngineImpl;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReader;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.FilesXmlService;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsService;
import nl.knaw.dans.validatedansbag.core.service.XmlReader;
import nl.knaw.dans.validatedansbag.core.service.XmlSchemaValidator;
import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidator;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidator;
import nl.knaw.dans.validatedansbag.core.validator.OrganizationIdentifierPrefixValidator;
import nl.knaw.dans.validatedansbag.core.validator.PolygonListValidator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuleSetsTest {

    private static final DataverseService dataverseService = Mockito.mock(DataverseService.class);
    private static final FileService fileService = Mockito.mock(FileService.class);

    private static final BagItMetadataReader bagItMetadataReader = Mockito.mock(BagItMetadataReader.class);

    private static final XmlSchemaValidator xmlSchemaValidator = Mockito.mock(XmlSchemaValidator.class);

    private static final LicenseValidator licenseValidator = Mockito.mock(LicenseValidator.class);
    private static final XmlReader xmlReader = Mockito.mock(XmlReader.class);

    private static final PolygonListValidator polygonListValidator = Mockito.mock(PolygonListValidator.class);

    private static final OriginalFilepathsService originalFilepathsService = Mockito.mock(OriginalFilepathsService.class);

    private static final FilesXmlService filesXmlService = Mockito.mock(FilesXmlService.class);

    private static final IdentifierValidator identifierValidator = Mockito.mock(IdentifierValidator.class);

    private static final OrganizationIdentifierPrefixValidator organizationIdentifierPrefixValidator = Mockito.mock(OrganizationIdentifierPrefixValidator.class);


    /*
     * The services in this test are never called; the only thing we want to test is whether the rule sets are consistent in terms of dependencies.
     * Therefore, the services are all mocked.
     */

    @Test
    public void dataStationsRuleSet_should_be_consistent() throws Exception {
        var ruleSets = new RuleSets(
                dataverseService, fileService, filesXmlService, originalFilepathsService, xmlReader,
                bagItMetadataReader, xmlSchemaValidator, licenseValidator, identifierValidator, polygonListValidator, organizationIdentifierPrefixValidator
        );
        new RuleEngineImpl().validateRuleConfiguration(ruleSets.getDataStationSet());
        assertTrue(true); // if we get here, the rule set is consistent
    }

    @Test
    public void vaasRuleSet_should_be_consistent() throws Exception {
        var ruleSets = new RuleSets(
                dataverseService, fileService, filesXmlService, originalFilepathsService, xmlReader,
                bagItMetadataReader, xmlSchemaValidator, licenseValidator, identifierValidator, polygonListValidator, organizationIdentifierPrefixValidator
        );
        new RuleEngineImpl().validateRuleConfiguration(ruleSets.getVaasSet());
        assertTrue(true); // if we get here, the rule set is consistent
    }

}
