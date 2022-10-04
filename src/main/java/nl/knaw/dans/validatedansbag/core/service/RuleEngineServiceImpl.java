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

import nl.knaw.dans.validatedansbag.core.BagNotFoundException;
import nl.knaw.dans.validatedansbag.core.engine.DepositType;
import nl.knaw.dans.validatedansbag.core.engine.NumberedRule;
import nl.knaw.dans.validatedansbag.core.engine.RuleEngine;
import nl.knaw.dans.validatedansbag.core.engine.RuleEngineConfigurationException;
import nl.knaw.dans.validatedansbag.core.engine.RuleValidationResult;
import nl.knaw.dans.validatedansbag.core.engine.ValidationContext;
import nl.knaw.dans.validatedansbag.core.engine.ValidationLevel;
import nl.knaw.dans.validatedansbag.core.rules.BagRules;
import nl.knaw.dans.validatedansbag.core.rules.DatastationRules;
import nl.knaw.dans.validatedansbag.core.rules.FilesXmlRules;
import nl.knaw.dans.validatedansbag.core.rules.XmlRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static nl.knaw.dans.validatedansbag.core.engine.NumberedRule.numberedRule;

public class RuleEngineServiceImpl implements RuleEngineService {
    private static final Logger log = LoggerFactory.getLogger(RuleEngineServiceImpl.class);
    private final RuleEngine ruleEngine;
    private final FileService fileService;
    private final NumberedRule[] defaultRules;
    private final Path datasetPath = Path.of("metadata/dataset.xml");
    private final Path metadataPath = Path.of("metadata");
    private final Path metadataFilesPath = Path.of("metadata/files.xml");

    public RuleEngineServiceImpl(RuleEngine ruleEngine, BagRules bagRules, XmlRules xmlRules, FilesXmlRules filesXmlRules, FileService fileService, DatastationRules datastationRules) {
        this.ruleEngine = ruleEngine;
        this.fileService = fileService;

        // validity
        this.defaultRules = new NumberedRule[] {
            numberedRule("1.1.1", bagRules.bagIsValid()).build(),

            // bag-info.txt
            numberedRule("1.2.1", bagRules.bagInfoExistsAndIsWellFormed()).build(),
            numberedRule("1.2.2(a)", bagRules.bagInfoContainsExactlyOneOf("Created"), "1.2.1").build(),
            numberedRule("1.2.2(b)", bagRules.bagInfoCreatedElementIsIso8601Date(), "1.2.2(a)").build(),
            numberedRule("1.2.3", bagRules.bagInfoContainsAtMostOneOf("Data-Station-User-Account"), "1.2.1").build(),
            numberedRule("1.2.4(a)", bagRules.bagInfoContainsAtMostOneOf("Is-Version-Of"), "1.2.1").build(),
            numberedRule("1.2.4(b)", bagRules.bagInfoIsVersionOfIsValidUrnUuid(), "1.2.4(a)").build(),
            numberedRule("1.2.5(a)", bagRules.bagInfoContainsAtMostOneOf("Has-Organizational-Identifier"), "1.2.1").build(),
            numberedRule("1.2.5(b)", bagRules.bagInfoContainsAtMostOneOf("Has-Organizational-Identifier-Version"), "1.2.5(a)").build(),

            // manifests
            numberedRule("1.3.1", bagRules.containsNotJustMD5Manifest(), "1.1.1").build(),

            // Structural
            numberedRule("2.1", bagRules.containsDir(metadataPath), "1.1.1").build(),
            numberedRule("2.2(a)", bagRules.containsFile(metadataPath.resolve("dataset.xml")), "2.1").build(),
            numberedRule("2.2(b)", bagRules.containsFile(metadataPath.resolve("files.xml")), "2.1").build(),

            // Both 2.4 rules also cover 2.2(c), 2.3 and 2.4
            numberedRule("2.4", bagRules.containsNothingElseThan(metadataPath, new String[] {
                "dataset.xml",
                "files.xml",
                "provenance.xml",
                "amd.xml",
                "emd.xml",
                "original",
                "original/dataset.xml",
                "original/files.xml",
                "depositor-info",
                "depositor-info/agreements.xml",
                "depositor-info/depositor-agreement.pdf",
                "depositor-info/message-from-depositor.txt",
                "license.html",
                "license.txt",
                "license.pdf"
            }), "2.1").withDepositType(DepositType.MIGRATION).build(),

            numberedRule("2.4", bagRules.containsNothingElseThan(metadataPath, new String[] {
                "dataset.xml",
                "files.xml"
            }), "2.1").withDepositType(DepositType.DEPOSIT).build(),

            numberedRule("2.5", bagRules.hasOnlyValidFileNames(), "2.1").build(),

            // original-filepaths.txt
            numberedRule("2.6.1", bagRules.optionalFileIsUtf8Decodable(Path.of("original-filepaths.txt")), "1.1.1").build(),
            numberedRule("2.6.2", bagRules.isOriginalFilepathsFileComplete(), "2.6.1").build(),

            // metadata/dataset.xml
            numberedRule("3.1.1", xmlRules.xmlFileConformsToSchema(datasetPath, "dataset.xml"), "1.1.1", "2.2(a)").build(),
            numberedRule("3.1.2", bagRules.ddmMayContainDctermsLicenseFromList(), "3.1.1").build(),
            numberedRule("3.1.3", bagRules.ddmDoiIdentifiersAreValid(), "3.1.1").build(),

            numberedRule("3.1.4(a)", bagRules.ddmDaisAreValid(), "3.1.1").build(),
            numberedRule("3.1.4(b)", bagRules.ddmIsnisAreValid(), "3.1.1").build(),
            numberedRule("3.1.4(c)", bagRules.ddmOrcidsAreValid(), "3.1.1").build(),
            numberedRule("3.1.5", bagRules.ddmGmlPolygonPosListIsWellFormed(), "3.1.1").build(),
            numberedRule("3.1.6", bagRules.polygonsInSameMultiSurfaceHaveSameSrsName(), "3.1.1").build(),
            numberedRule("3.1.7", bagRules.pointsHaveAtLeastTwoValues(), "3.1.1").build(),
            numberedRule("3.1.8", bagRules.archisIdentifiersHaveAtMost10Characters(), "3.1.1").build(),
            numberedRule("3.1.9", bagRules.allUrlsAreValid(), "3.1.1").build(),

            numberedRule("3.1.10(a)", bagRules.ddmMustHaveRightsHolderDeposit(), "3.1.1").withDepositType(DepositType.DEPOSIT).build(),
            numberedRule("3.1.10(b)", bagRules.ddmMustHaveRightsHolderDeposit(), "3.1.1").withDepositType(DepositType.MIGRATION).build(),
            numberedRule("3.1.11", bagRules.ddmMustNotHaveRightsHolderRole(), "3.1.1").withDepositType(DepositType.DEPOSIT).build(),

            numberedRule("3.2.1", xmlRules.xmlFileConformsToSchema(metadataFilesPath, "files.xml"), "3.1.1").build(),
            numberedRule("3.2.2", filesXmlRules.filesXmlFilePathAttributesContainLocalBagPathAndNonPayloadFilesAreNotDescribed(), "2.2(b)").build(),
            numberedRule("3.2.3", filesXmlRules.filesXmlNoDuplicateFilesAndEveryPayloadFileIsDescribed(), "2.2(b)").build(),

            // agreements.xml
            numberedRule("3.3.1", xmlRules.xmlFileIfExistsConformsToSchema(Path.of("metadata/depositor-info/agreements.xml"), "agreements.xml"))
                .withDepositType(DepositType.MIGRATION).build(),

            // amd.xml
            numberedRule("3.3.2", xmlRules.xmlFileIfExistsConformsToSchema(Path.of("metadata/amd.xml"), "amd.xml")).withDepositType(DepositType.MIGRATION).build(),

            // emd.xml
            numberedRule("3.3.3", xmlRules.xmlFileIfExistsConformsToSchema(Path.of("metadata/emd.xml"), "emd.xml")).withDepositType(DepositType.MIGRATION).build(),

            // provenance.xml
            numberedRule("3.3.4", xmlRules.xmlFileIfExistsConformsToSchema(Path.of("metadata/provenance.xml"), "provenance.xml")).withDepositType(DepositType.MIGRATION).build(),

            numberedRule("4.1", bagRules.bagInfoContainsExactlyOneOf("Data-Station-User-Account"), "1.2.1")
                .withValidationContext(ValidationContext.WITH_DATA_STATION_CONTEXT).build(),

            numberedRule("4.2", datastationRules.userIsAuthorizedToCreateDataset(), "4.1")
                .withValidationContext(ValidationContext.WITH_DATA_STATION_CONTEXT).build(),

            numberedRule("4.3", bagRules.organizationalIdentifierPrefixIsValid(), "4.1", "1.2.5(a)")
                .withValidationContext(ValidationContext.WITH_DATA_STATION_CONTEXT).build(),

            numberedRule("4.4(a)", datastationRules.bagExistsInDatastation(), "4.1")
                .withValidationContext(ValidationContext.WITH_DATA_STATION_CONTEXT).build(),

            numberedRule("4.4(b)", datastationRules.organizationalIdentifierExistsInDataset(), "4.1", "4.4(a)")
                .withValidationContext(ValidationContext.WITH_DATA_STATION_CONTEXT).build(),

            numberedRule("4.4(c)", datastationRules.userIsAuthorizedToUpdateDataset(), "4.1", "4.4(a)")
                .withValidationContext(ValidationContext.WITH_DATA_STATION_CONTEXT).build()
        };

        this.validateRuleConfiguration();
    }

    @Override
    public List<RuleValidationResult> validateBag(Path path, DepositType depositType, ValidationLevel validationLevel) throws Exception {
        log.info("Validating bag on path '{}', deposit type is {} and validation level {}", path, depositType, validationLevel);

        if (!fileService.isReadable(path)) {
            log.warn("Path {} could not not be found or is not readable", path);
            throw new BagNotFoundException(String.format("Bag on path '%s' could not be found or read", path));
        }

        return ruleEngine.validateRules(path, this.defaultRules, depositType, validationLevel);
    }

    public void validateRuleConfiguration() {
        try {
            this.ruleEngine.validateRuleConfiguration(this.defaultRules);
        }
        catch (RuleEngineConfigurationException e) {
            throw new RuntimeException("Rule configuration is not valid", e);
        }
    }
}
