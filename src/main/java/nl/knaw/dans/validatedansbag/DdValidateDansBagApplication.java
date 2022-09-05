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
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.dans.validatedansbag.core.engine.RuleEngineImpl;
import nl.knaw.dans.validatedansbag.core.rules.BagRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.DatastationRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.FilesXmlRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.XmlRulesImpl;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.DataverseServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.FileServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.RuleEngineServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlSchemaValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.PolygonListValidatorImpl;
import nl.knaw.dans.validatedansbag.resource.ValidateOkDtoYamlMessageBodyWriter;
import nl.knaw.dans.validatedansbag.resource.ValidateResource;

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
        bootstrap.addBundle(new MultiPartBundle());
    }

    @Override
    public void run(final DdValidateDansBagConfiguration configuration, final Environment environment) {

        var fileService = new FileServiceImpl();
        var bagItMetadataReader = new BagItMetadataReaderImpl();
        var xmlReader = new XmlReaderImpl();
        var daiDigestCalculator = new IdentifierValidatorImpl();
        var polygonListValidator = new PolygonListValidatorImpl();
        var originalFilepathsService = new OriginalFilepathsServiceImpl(fileService);
        var licenseValidator = new LicenseValidatorImpl(configuration.getLicenses());

        var xmlSchemaValidator = new XmlSchemaValidatorImpl();

        var dataverseService = new DataverseServiceImpl(configuration.getDataverse());

        // set up the different rule implementations
        var bagRules = new BagRulesImpl(fileService, bagItMetadataReader, xmlReader, originalFilepathsService, daiDigestCalculator, polygonListValidator, licenseValidator);
        var filesXmlRules = new FilesXmlRulesImpl(xmlReader, fileService, originalFilepathsService);
        var xmlRules = new XmlRulesImpl(xmlReader, xmlSchemaValidator, fileService);
        var datastationRules = new DatastationRulesImpl(bagItMetadataReader, dataverseService);

        // set up the engine and the service that has a default set of rules
        var ruleEngine = new RuleEngineImpl();
        var ruleEngineService = new RuleEngineServiceImpl(ruleEngine, bagRules, xmlRules, filesXmlRules, fileService, datastationRules);

        environment.jersey().register(new ValidateResource(ruleEngineService, fileService));
        environment.jersey().register(new ValidateOkDtoYamlMessageBodyWriter());
    }
}
