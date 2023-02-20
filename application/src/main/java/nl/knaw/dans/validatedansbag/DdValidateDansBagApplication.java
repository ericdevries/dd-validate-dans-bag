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
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.dans.validatedansbag.core.auth.SwordAuthenticator;
import nl.knaw.dans.validatedansbag.core.auth.SwordUser;
import nl.knaw.dans.validatedansbag.core.engine.RuleEngineImpl;
import nl.knaw.dans.validatedansbag.core.rules.BagRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.DatastationRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.FilesXmlRulesImpl;
import nl.knaw.dans.validatedansbag.core.rules.XmlRulesImpl;
import nl.knaw.dans.validatedansbag.core.service.BagItMetadataReaderImpl;
import nl.knaw.dans.validatedansbag.core.service.BagOwnerValidatorImpl;
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

        var fileService = new FileServiceImpl();
        var bagItMetadataReader = new BagItMetadataReaderImpl();
        var xmlReader = new XmlReaderImpl();
        var daiDigestCalculator = new IdentifierValidatorImpl();
        var polygonListValidator = new PolygonListValidatorImpl();
        var originalFilepathsService = new OriginalFilepathsServiceImpl(fileService);
        var licenseValidator = new LicenseValidatorImpl(configuration.getValidationConfig().getLicenseConfig());
        var filesXmlService = new FilesXmlServiceImpl(xmlReader);

        var xmlSchemaValidator = new XmlSchemaValidatorImpl(configuration.getValidationConfig().getXmlSchemas().buildMap());

        var dataverseService = new DataverseServiceImpl(configuration.getDataverse().build());

        var organizationIdentifierPrefixValidator = new OrganizationIdentifierPrefixValidatorImpl(configuration.getValidationConfig().getOtherIdPrefixes());

        // set up the different rule implementations
        var bagRules = new BagRulesImpl(fileService, bagItMetadataReader, xmlReader, originalFilepathsService, daiDigestCalculator, polygonListValidator, licenseValidator,
            organizationIdentifierPrefixValidator, filesXmlService);
        var filesXmlRules = new FilesXmlRulesImpl(fileService, originalFilepathsService, filesXmlService);
        var xmlRules = new XmlRulesImpl(xmlReader, xmlSchemaValidator, fileService);
        var datastationRules = new DatastationRulesImpl(bagItMetadataReader, dataverseService, configuration.getValidationConfig().getSwordDepositorRoles(), xmlReader);

        // set up the engine and the service that has a default set of rules
        var ruleEngine = new RuleEngineImpl();
        var ruleEngineService = new RuleEngineServiceImpl(ruleEngine, bagRules, xmlRules, filesXmlRules, fileService, datastationRules);

        var validationConfig = configuration.getValidationConfig();

        // the http client for making authentication calls
        var httpClient = new HttpClientBuilder(environment)
            .using(validationConfig.getHttpClientConfiguration())
            .build(getName());

        // set up authentication
        var swordAuthenticator = new SwordAuthenticator(validationConfig.getPasswordDelegate(), httpClient);

        // bag owner validation step
        var bagOwnerValidator = new BagOwnerValidatorImpl(bagItMetadataReader);

        // register the authentication plugins from dropwizard
        environment.jersey().register(
            new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<SwordUser>()
                .setAuthenticator(swordAuthenticator)
                .setRealm(validationConfig.getPasswordRealm())
                .buildAuthFilter())
        );

        // for @Auth
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(SwordUser.class));

        environment.jersey().register(new IllegalArgumentExceptionMapper());
        environment.jersey().register(new ValidateResource(ruleEngineService, fileService, bagOwnerValidator));
        environment.jersey().register(new ValidateOkYamlMessageBodyWriter());

        environment.healthChecks().register("xml-schemas", new XmlSchemaHealthCheck(xmlSchemaValidator));
        environment.healthChecks().register("dataverse", new DataverseHealthCheck(dataverseService));
    }
}
