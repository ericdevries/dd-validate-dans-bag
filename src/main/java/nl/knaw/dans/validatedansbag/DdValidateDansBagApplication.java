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

package nl.knaw.dans.validatedansbag;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.dans.validatedansbag.core.engine.DepositType;
import nl.knaw.dans.validatedansbag.core.engine.NumberedRule;
import nl.knaw.dans.validatedansbag.core.engine.RuleEngineImpl;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.IdentifierValidatorImpl;
import nl.knaw.dans.validatedansbag.core.service.FileServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.LicenseValidatorImpl;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.PolygonListValidatorImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlSchemaValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.BagValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.FilesXmlValidatorImpl;
import org.xml.sax.SAXException;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

public class DdValidateDansBagApplication extends Application<DdValidateDansBagConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DdValidateDansBagApplication().run(args);
    }

    @Override
    public String getName() {
        return "Dd Validate Dans Bag";
    }

    @Override
    public void initialize(final Bootstrap<DdValidateDansBagConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final DdValidateDansBagConfiguration configuration, final Environment environment) throws MalformedURLException, SAXException {

        var fileService = new FileServiceImpl();
        var bagItMetadataReader = new BagItMetadataReaderImpl();
        var bagXmlReader = new XmlReaderImpl();
        var daiDigestCalculator = new IdentifierValidatorImpl();
        var polygonListValidator = new PolygonListValidatorImpl();
        var originalFilepathsService = new OriginalFilepathsServiceImpl(fileService);
        var licenseValidator = new LicenseValidatorImpl();

        var xmlValidator = new XmlSchemaValidatorImpl();

        var validator = new BagValidatorImpl(fileService, bagItMetadataReader, bagXmlReader, originalFilepathsService, daiDigestCalculator, polygonListValidator, xmlValidator, licenseValidator);

        var filesXmlValidator = new FilesXmlValidatorImpl(bagXmlReader, fileService, originalFilepathsService);

        var dataPath = Path.of("data");
        var datasetPath = Path.of("metadata/dataset.xml");
        var metadataPath = Path.of("metadata");
        var metadataFilesPath = Path.of("metadata/files.xml");

        var rules = new NumberedRule[] {
            // validity
            new NumberedRule("1.1.1", validator.bagIsValid()),
            new NumberedRule("1.1.1(datadir)", validator.containsDir(dataPath)),

            // bag-info.txt
            new NumberedRule("1.2.1", validator.bagInfoExistsAndIsWellFormed()),
            new NumberedRule("1.2.2(a)", validator.bagInfoContainsExactlyOneOf("Created"), List.of("1.2.1")),
            new NumberedRule("1.2.2(b)", validator.bagInfoCreatedElementIsIso8601Date(), List.of("1.2.2(a)")),
            new NumberedRule("1.2.3", validator.bagInfoContainsAtMostOneOf("Data-Station-User-Account"), List.of("1.2.1")),
            new NumberedRule("1.2.4(a)", validator.bagInfoContainsAtMostOneOf("Is-Version-Of"), List.of("1.2.1")),
            new NumberedRule("1.2.4(b)", validator.bagInfoIsVersionOfIsValidUrnUuid(), List.of("1.2.4(a)")),
            new NumberedRule("1.2.5(a)", validator.bagInfoContainsAtMostOneOf("Has-Organizational-Identifier"), List.of("1.2.1")),
            // TODO 1.2.5 it needs to verify other bags as well
            new NumberedRule("1.2.5(b)", validator.bagInfoContainsAtMostOneOf("Has-Organizational-Identifier"), List.of("1.2.5(a)")),

            // manifests
            new NumberedRule("1.3.1(a)", validator.containsFile(Path.of("manifest-sha1.txt"))),
            new NumberedRule("1.3.1(b)", validator.bagShaPayloadManifestContainsAllPayloadFiles()),

            // Structural
            new NumberedRule("2.1", validator.containsDir(metadataPath)),
            new NumberedRule("2.2(a)", validator.containsDir(metadataPath.resolve("dataset.xml"))),
            new NumberedRule("2.2(b)", validator.containsDir(metadataPath.resolve("files.xml"))),

            // this also covers 2.3 and 2.4 for MIGRATION status deposits
            new NumberedRule("2.2(c)", validator.containsNothingElseThan(metadataPath, new String[] {
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

            new NumberedRule("2.4(deposit)", validator.containsNothingElseThan(metadataPath, new String[] {
                "dataset.xml",
                "files.xml"
            }), DepositType.DEPOSIT, List.of("2.1")),

            new NumberedRule("2.5", validator.hasOnlyValidFileNames(), List.of("2.1")),

            // original-filepaths.txt
            new NumberedRule("2.6.1", validator.optionalFileIsUtf8Decodable(Path.of("original-filepaths.txt")), List.of("1.1.1(datadir)")),
            new NumberedRule("2.6.2(a)", validator.originalFilePathsDoNotContainSpaces(), List.of("1.1.1(datadir)", "2.6.1", "2.2(b)")),
            new NumberedRule("2.6.2(b)", validator.isOriginalFilepathsFileComplete(), List.of("2.6.2(a)")),

            // metadata/dataset.xml
            new NumberedRule("3.1.1", validator.xmlFileConfirmsToSchema(datasetPath, "dataset.xml"), List.of("2.2(a)")),
            new NumberedRule("3.1.2", validator.ddmMayContainDctermsLicenseFromList(), List.of("3.1.1")),
//            new NumberedRule("3.1.3(a)", validator.ddmContainsUrnNbnIdentifier(), List.of("3.1.1")),
            new NumberedRule("3.1.3", validator.ddmDoiIdentifiersAreValid(), List.of("3.1.1")),
            // TODO check how we can validate each kind of identifier (assumptions about ISNI and ORCID are made now based on wikipedia)
            new NumberedRule("3.1.4(a)", validator.ddmDaisAreValid(), List.of("3.1.1")),
            new NumberedRule("3.1.4(b)", validator.ddmIsnisAreValid(), List.of("3.1.1")),
            new NumberedRule("3.1.4(c)", validator.ddmOrcidsAreValid(), List.of("3.1.1")),
            new NumberedRule("3.1.5", validator.ddmGmlPolygonPosListIsWellFormed(), List.of("3.1.1")),
            new NumberedRule("3.1.6", validator.polygonsInSameMultiSurfaceHaveSameSrsName(), List.of("3.1.1")),
            new NumberedRule("3.1.7", validator.pointsHaveAtLeastTwoValues(), List.of("3.1.1")),
            new NumberedRule("3.1.8", validator.archisIdentifiersHaveAtMost10Characters(), List.of("3.1.1")),
            new NumberedRule("3.1.9", validator.allUrlsAreValid(), List.of("3.1.1")),
            // TODO differentiate between MIGRATE and DEPOSIT
            new NumberedRule("3.1.10", validator.ddmMustHaveRightsHolder(), List.of("3.1.1")),

            new NumberedRule("3.2.1", validator.xmlFileConfirmsToSchema(metadataFilesPath, "files.xml"), List.of("3.1.1")),
            new NumberedRule("3.2.2", filesXmlValidator.filesXmlHasDocumentElementFiles(), List.of("2.2(b)")),
            new NumberedRule("3.2.3", filesXmlValidator.filesXmlHasOnlyFiles(), List.of("3.2.2")),
            new NumberedRule("3.2.4", filesXmlValidator.filesXmlFileElementsAllHaveFilepathAttribute(), List.of("3.2.3")),
            new NumberedRule("3.2.5", filesXmlValidator.filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFiles(), List.of("1.1.1(datadir)", "3.2.4")),

            new NumberedRule("3.2.6", filesXmlValidator.filesXmlAllFilesHaveFormat(), List.of("3.2.2")),
            new NumberedRule("3.2.7", filesXmlValidator.filesXmlFilesHaveOnlyAllowedNamespaces(), List.of("3.2.2")),
            new NumberedRule("3.2.8", filesXmlValidator.filesXmlFilesHaveOnlyAllowedAccessRights(), List.of("3.2.2")),

            // TODO these should only apply to MIGRATION status
            // agreements.xml
            new NumberedRule("3.3.1", validator.xmlFileIfExistsConformsToSchema(Path.of("metadata/depositor-info/agreements.xml"), "agreements.xml")),

            // amd.xml
            new NumberedRule("3.6.1", validator.xmlFileIfExistsConformsToSchema(Path.of("metadata/amd.xml"), "amd.xml")),

            // emd.xml
            new NumberedRule("3.7.1", validator.xmlFileIfExistsConformsToSchema(Path.of("metadata/emd.xml"), "emd.xml")),

            // provenance.xml
            new NumberedRule("3.8.1", validator.xmlFileIfExistsConformsToSchema(Path.of("metadata/provenance.xml"), "provenance.xml")),

            // TODO 4.x rules should check for the existence of certain users in another system
        };

        var path = Path.of("/home/eric/workspace/dd-validate-dans-bag/src/test/resources/original-filepaths-valid-bag");

        var engine = new RuleEngineImpl();
        var result = engine.validateRules(path, rules);
    }

}
