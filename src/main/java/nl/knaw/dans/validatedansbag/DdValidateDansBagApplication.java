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
import nl.knaw.dans.validatedansbag.core.service.BagInfoCheckerImpl;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.BagXmlReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.DaiDigestCalculatorImpl;
import nl.knaw.dans.validatedansbag.core.service.FileServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.NumberedRule;
import nl.knaw.dans.validatedansbag.core.service.PolygonListValidatorImpl;
import nl.knaw.dans.validatedansbag.core.service.RuleEngineImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlValidatorImpl;
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
        var bagXmlReader = new BagXmlReaderImpl();
        var daiDigestCalculator = new DaiDigestCalculatorImpl();
        var polygonListValidator = new PolygonListValidatorImpl();

        var xmlValidator = new XmlValidatorImpl();

        var validator = new BagInfoCheckerImpl(fileService, bagItMetadataReader, bagXmlReader, daiDigestCalculator, polygonListValidator, xmlValidator);

        var rules = new NumberedRule[] {
            // validity
            new NumberedRule("1.1.1", validator.bagIsValid()),
            new NumberedRule("1.1.1(datadir)", validator.containsDir(Path.of("data"))),

            // bag-info.txt
            new NumberedRule("1.2.1", validator.bagInfoExistsAndIsWellFormed()),
            new NumberedRule("1.2.4(a)", validator.bagInfoContainsExactlyOneOf("Created"), List.of("1.2.1")),
            new NumberedRule("1.2.4(b)", validator.bagInfoCreatedElementIsIso8601Date(), List.of("1.2.4(a)")),
            new NumberedRule("1.2.5", validator.bagInfoContainsAtMostOneOf("Is-Version-Of"), List.of("1.2.1")),

            // manifests
            new NumberedRule("1.3.1(a)", validator.containsFile(Path.of("manifest-sha1.txt"))),
            new NumberedRule("1.3.1(b)", validator.bagShaPayloadManifestContainsAllPayloadFiles()),

            // Structural
            new NumberedRule("2.1", validator.containsDir(Path.of("metadata"))),
            new NumberedRule("2.2(a)", validator.containsFile(Path.of("metadata/dataset.xml")), List.of("2.1")),
            new NumberedRule("2.2(b)", validator.containsFile(Path.of("metadata/files.xml")), List.of("2.1")),
            new NumberedRule("2.5", validator.containsNothingElseThan(Path.of("metadata"), new String[] {
                "dataset.xml",
                "provenance.xml",
                "pre-staged.csv",
                "files.xml",
                "amd.xml",
                "emd.xml",
                "license.txt",
                "license.pdf",
                "license.html",
                "depositor-info",
                "original",
                "original/dataset.xml",
                "original/files.xml",
                "depositor-info/agreements.xml",
                "depositor-info/message-from-depositor.txt",
                "depositor-info/depositor-agreement.pdf",
                "depositor-info/depositor-agreement.txt",
            }), List.of("2.1")),

            new NumberedRule("2.6", validator.hasOnlyValidFileNames(), List.of("1.3.1(b)")),
            new NumberedRule("2.7.1", validator.optionalFileIsUtf8Decodable(Path.of("original-filepaths.txt"))),
            new NumberedRule("2.7.2", validator.isOriginalFilepathsFileComplete(), List.of("1.1.1(datadir)", "2.7.1", "2.2(b)", "3.2.4")),

            new NumberedRule("3.1.1", validator.xmlFileConfirmsToSchema(Path.of("metadata/dataset.xml"), "dataset.xml"), List.of("2.2(a)")),
            new NumberedRule("3.1.2", validator.ddmMayContainDctermsLicenseFromList(), List.of("3.1.1")),
            new NumberedRule("3.1.3(a)", validator.ddmContainsUrnNbnIdentifier(), List.of("3.1.1")),
            new NumberedRule("3.1.3(b)", validator.ddmDoiIdentifiersAreValid(), List.of("3.1.1")),
            new NumberedRule("3.1.4", validator.ddmDaisAreValid(), List.of("3.1.1")),
            new NumberedRule("3.1.5", validator.ddmGmlPolygonPosListIsWellFormed(), List.of("3.1.1")),
            new NumberedRule("3.1.6", validator.polygonsInSameMultiSurfaceHaveSameSrsName(), List.of("3.1.1")),
            new NumberedRule("3.1.7", validator.pointsHaveAtLeastTwoValues(), List.of("3.1.1")),
            new NumberedRule("3.1.8", validator.archisIdentifiersHaveAtMost10Characters(), List.of("3.1.1")),
            new NumberedRule("3.1.9", validator.allUrlsAreValid(), List.of("3.1.1")),
            new NumberedRule("3.1.10", validator.ddmMustHaveRightsHolder(), List.of("3.1.1")),

            new NumberedRule("3.2.1", validator.xmlFileConfirmsToSchema(Path.of("metadata/files.xml"), "files.xml"), List.of("3.1.1")),
            new NumberedRule("3.2.2", validator.filesXmlHasDocumentElementFiles(), List.of("2.2(b)")),
            new NumberedRule("3.2.3", validator.filesXmlHasOnlyFiles(), List.of("3.2.2")),
            new NumberedRule("3.2.4", validator.filesXmlFileElementsAllHaveFilepathAttribute(), List.of("3.2.3")),
            new NumberedRule("3.2.5", validator.filesXmlNoDuplicatesAndMatchesWithPayloadPlusPreStagedFiles(), List.of("1.1.1(datadir)", "3.2.4")),

            new NumberedRule("3.2.6", validator.filesXmlAllFilesHaveFormat(), List.of("3.2.2")),
            new NumberedRule("3.2.7", validator.filesXmlFilesHaveOnlyAllowedNamespaces(), List.of("3.2.2")),
            new NumberedRule("3.2.8", validator.filesXmlFilesHaveOnlyAllowedAccessRights(), List.of("3.2.2")),

            // agreements.xml
            new NumberedRule("3.3.1", validator.xmlFileIfExistsConformsToSchema(Path.of("metadata/depositor-info/agreements.xml"), "agreements.xml")),

            // amd.xml
            new NumberedRule("3.6.1", validator.xmlFileIfExistsConformsToSchema(Path.of("metadata/amd.xml"), "amd.xml")),

            // emd.xml
            new NumberedRule("3.7.1", validator.xmlFileIfExistsConformsToSchema(Path.of("metadata/emd.xml"), "emd.xml")),

            // provenance.xml
            new NumberedRule("3.8.1", validator.xmlFileIfExistsConformsToSchema(Path.of("metadata/provenance.xml"), "provenance.xml")),
        };

        var path = Path.of("/home/eric/workspace/dd-validate-dans-bag/src/test/resources/valid-bag");

        var engine = new RuleEngineImpl();
        var result = engine.validateRules(path, rules);
    }

}
