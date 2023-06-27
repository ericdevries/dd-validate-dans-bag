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
import nl.knaw.dans.validatedansbag.core.rules.RuleSets;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.DataverseService;
import nl.knaw.dans.validatedansbag.core.service.DataverseServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.FileServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.FilesXmlServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.OriginalFilepathsServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.RuleEngineServiceImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.XmlSchemaValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.LicenseValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.OrganizationIdentifierPrefixValidatorImpl;
import nl.knaw.dans.validatedansbag.core.validator.PolygonListValidatorImpl;
import nl.knaw.dans.validatedansbag.health.DataverseHealthCheck;
import nl.knaw.dans.validatedansbag.health.XmlSchemaHealthCheck;
import nl.knaw.dans.validatedansbag.resources.IllegalArgumentExceptionMapper;
import nl.knaw.dans.validatedansbag.resources.ValidateOkYamlMessageBodyWriter;
import nl.knaw.dans.validatedansbag.resources.ValidateResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DdValidateDansBagApplication extends Application<DdValidateDansBagConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(DdValidateDansBagApplication.class);

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
        validateContextConfiguration(configuration);
        DataverseService dataverseService = null;

        if (configuration.getDataverse() != null) {
            dataverseService = new DataverseServiceImpl(configuration.getDataverse().build());
        }

        var fileService = new FileServiceImpl();
        var bagItMetadataReader = new BagItMetadataReaderImpl();
        var xmlReader = new XmlReaderImpl();
        var polygonListValidator = new PolygonListValidatorImpl();
        var originalFilepathsService = new OriginalFilepathsServiceImpl(fileService);
        var filesXmlService = new FilesXmlServiceImpl(xmlReader);
        var xmlSchemaValidator = new XmlSchemaValidatorImpl(configuration.getValidation().getXmlSchemas().buildMap());

        var licenseValidator = new LicenseValidatorImpl(dataverseService);
        var identifierValidator = new IdentifierValidatorImpl();
        var organizationIdentifierPrefixValidator = new OrganizationIdentifierPrefixValidatorImpl(configuration.getValidation().getOtherIdPrefixes());

        var ruleEngine = new RuleEngineImpl();
        var ruleSets = new RuleSets(dataverseService,
                fileService,
                filesXmlService,
                originalFilepathsService,
                xmlReader,
                bagItMetadataReader,
                xmlSchemaValidator,
                licenseValidator,
                identifierValidator,
                polygonListValidator,
                organizationIdentifierPrefixValidator);
        var ruleEngineService = new RuleEngineServiceImpl(ruleEngine, fileService,
                configuration.getDataverse() != null ? ruleSets.getDataStationSet() : ruleSets.getVaasSet());

        environment.jersey().register(new IllegalArgumentExceptionMapper());
        environment.jersey().register(new ValidateResource(ruleEngineService, fileService));
        environment.jersey().register(new ValidateOkYamlMessageBodyWriter());

        environment.healthChecks().register("xml-schemas", new XmlSchemaHealthCheck(xmlSchemaValidator));
        environment.healthChecks().register("dataverse", new DataverseHealthCheck(dataverseService));
    }

    private void validateContextConfiguration(DdValidateDansBagConfiguration configuration) {
        if ((configuration.getDataverse() != null) == (configuration.getVaultCatalog() != null)) {
            throw new IllegalArgumentException("Exactly one of dataverse and vaultCatalog must be configured");
        }
    }
}
