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
            new NumberedRule("1.1.1", bagRules.bagIsValid()),

            // bag-info.txt
            new NumberedRule("1.2.1", bagRules.bagInfoExistsAndIsWellFormed()),
            new NumberedRule("1.2.2(a)", bagRules.bagInfoContainsExactlyOneOf("Created"), List.of("1.2.1")),
            new NumberedRule("1.2.2(b)", bagRules.bagInfoCreatedElementIsIso8601Date(), List.of("1.2.2(a)")),
            new NumberedRule("1.2.3", bagRules.bagInfoContainsAtMostOneOf("Data-Station-User-Account"), List.of("1.2.1")),
            new NumberedRule("1.2.4(a)", bagRules.bagInfoContainsAtMostOneOf("Is-Version-Of"), List.of("1.2.1")),
            new NumberedRule("1.2.4(b)", bagRules.bagInfoIsVersionOfIsValidUrnUuid(), List.of("1.2.4(a)")),
            new NumberedRule("1.2.5(a)", bagRules.bagInfoContainsAtMostOneOf("Has-Organizational-Identifier"), List.of("1.2.1")),
            new NumberedRule("1.2.5(b)", bagRules.bagInfoContainsAtMostOneOf("Has-Organizational-Identifier-Version"), List.of("1.2.5(a)")),

            // manifests
            new NumberedRule("1.3.1", bagRules.containsNotJustMD5Manifest(), List.of("1.1.1")),

            // Structural
            new NumberedRule("2.1", bagRules.containsDir(metadataPath), List.of("1.1.1")),
            new NumberedRule("2.2(a)", bagRules.containsFile(metadataPath.resolve("dataset.xml")), List.of("2.1")),
            new NumberedRule("2.2(b)", bagRules.containsFile(metadataPath.resolve("files.xml")), List.of("2.1")),

            // this also covers 2.3 and 2.4 for MIGRATION status deposits
            new NumberedRule("2.4", bagRules.containsNothingElseThan(metadataPath, new String[] {
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
            }), DepositType.MIGRATION, List.of("2.1")),

            new NumberedRule("2.4", bagRules.containsNothingElseThan(metadataPath, new String[] {
                "dataset.xml",
                "files.xml"
            }), DepositType.DEPOSIT, List.of("2.1")),

            new NumberedRule("2.5", bagRules.hasOnlyValidFileNames(), List.of("2.1")),

            // original-filepaths.txt
            new NumberedRule("2.6.1", bagRules.optionalFileIsUtf8Decodable(Path.of("original-filepaths.txt")), List.of("1.1.1")),
            new NumberedRule("2.6.2", bagRules.isOriginalFilepathsFileComplete(), List.of("2.6.1")),

            // metadata/dataset.xml
            new NumberedRule("3.1.1", xmlRules.xmlFileConfirmsToSchema(datasetPath, "dataset.xml"), List.of("1.1.1", "2.2(a)")),
            new NumberedRule("3.1.2", bagRules.ddmMayContainDctermsLicenseFromList(), List.of("3.1.1")),
            new NumberedRule("3.1.3", bagRules.ddmDoiIdentifiersAreValid(), List.of("3.1.1")),

            new NumberedRule("3.1.4(a)", bagRules.ddmDaisAreValid(), List.of("3.1.1")),
            new NumberedRule("3.1.4(b)", bagRules.ddmIsnisAreValid(), List.of("3.1.1")),
            new NumberedRule("3.1.4(c)", bagRules.ddmOrcidsAreValid(), List.of("3.1.1")),
            new NumberedRule("3.1.5", bagRules.ddmGmlPolygonPosListIsWellFormed(), List.of("3.1.1")),
            new NumberedRule("3.1.6", bagRules.polygonsInSameMultiSurfaceHaveSameSrsName(), List.of("3.1.1")),
            new NumberedRule("3.1.7", bagRules.pointsHaveAtLeastTwoValues(), List.of("3.1.1")),
            new NumberedRule("3.1.8", bagRules.archisIdentifiersHaveAtMost10Characters(), List.of("3.1.1")),
            new NumberedRule("3.1.9", bagRules.allUrlsAreValid(), List.of("3.1.1")),

            new NumberedRule("3.1.10(a)", bagRules.ddmMustHaveRightsHolderDeposit(), DepositType.DEPOSIT, List.of("3.1.1")),
            new NumberedRule("3.1.10(b)", bagRules.ddmMustHaveRightsHolderMigration(), DepositType.MIGRATION, List.of("3.1.1")),

            new NumberedRule("3.2.1", xmlRules.xmlFileConfirmsToSchema(metadataFilesPath, "files.xml"), List.of("3.1.1")),
            new NumberedRule("3.2.2", filesXmlRules.filesXmlFilePathAttributesContainLocalBagPathAndNonPayloadFilesAreNotDescribed(), List.of("2.2(b)")),
            new NumberedRule("3.2.3", filesXmlRules.filesXmlNoDuplicateFilesAndEveryPayloadFileIsDescribed(), List.of("2.2(b)")),

            // agreements.xml
            new NumberedRule("3.3.1", xmlRules.xmlFileIfExistsConformsToSchema(Path.of("metadata/depositor-info/agreements.xml"), "agreements.xml"), DepositType.MIGRATION),

            // amd.xml
            new NumberedRule("3.3.2", xmlRules.xmlFileIfExistsConformsToSchema(Path.of("metadata/amd.xml"), "amd.xml"), DepositType.MIGRATION),

            // emd.xml
            new NumberedRule("3.3.3", xmlRules.xmlFileIfExistsConformsToSchema(Path.of("metadata/emd.xml"), "emd.xml"), DepositType.MIGRATION),

            // provenance.xml
            new NumberedRule("3.3.4", xmlRules.xmlFileIfExistsConformsToSchema(Path.of("metadata/provenance.xml"), "provenance.xml"), DepositType.MIGRATION),

            new NumberedRule("4.1", bagRules.bagInfoContainsAtMostOneOf("Data-Station-User-Account"), ValidationContext.WITH_DATA_STATION_CONTEXT, List.of("1.2.1")),
            new NumberedRule("4.2", datastationRules.userIsAuthorizedToCreateDataset(), ValidationContext.WITH_DATA_STATION_CONTEXT, List.of("4.1")),
            new NumberedRule("4.3", bagRules.organizationalIdentifierPrefixIsValid(), ValidationContext.WITH_DATA_STATION_CONTEXT, List.of("4.1", "1.2.5(a)")),
            new NumberedRule("4.4(a)", datastationRules.bagExistsInDatastation(), ValidationContext.WITH_DATA_STATION_CONTEXT, List.of("4.1")),
            new NumberedRule("4.4(b)", datastationRules.organizationalIdentifierExistsInDataset(), ValidationContext.WITH_DATA_STATION_CONTEXT, List.of("4.1")),
            new NumberedRule("4.4(c)", datastationRules.userIsAuthorizedToUpdateDataset(), ValidationContext.WITH_DATA_STATION_CONTEXT, List.of("4.1")),
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
