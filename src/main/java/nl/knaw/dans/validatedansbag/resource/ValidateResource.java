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
package nl.knaw.dans.validatedansbag.resource;

import nl.knaw.dans.openapi.api.ValidateCommandDto;
import nl.knaw.dans.openapi.api.ValidateOkDto;
import nl.knaw.dans.openapi.api.ValidateOkRuleViolationsDto;
import nl.knaw.dans.validatedansbag.core.BagNotFoundException;
import nl.knaw.dans.validatedansbag.core.engine.DepositType;
import nl.knaw.dans.validatedansbag.core.engine.RuleValidationResult;
import nl.knaw.dans.validatedansbag.core.engine.ValidationLevel;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.RuleEngineService;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

@Path("/validate")
public class ValidateResource {

    private static final Logger log = LoggerFactory.getLogger(ValidateResource.class);

    private final RuleEngineService ruleEngineService;

    private final FileService fileService;

    public ValidateResource(RuleEngineService ruleEngineService, FileService fileService) {
        this.ruleEngineService = ruleEngineService;
        this.fileService = fileService;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateFormData(
        @Valid @NotNull @FormDataParam(value = "command") ValidateCommandDto command,
        @FormDataParam(value = "zip") InputStream zipInputStream
    ) {
        var location = command.getBagLocation();
        var depositType = toDepositType(command.getPackageType());
        var validationLevel = toValidationLevel(command.getLevel());

        try {
            ValidateOkDto validateResult;

            if (location == null) {
                validateResult = validateInputStream(zipInputStream, depositType, validationLevel);
            }
            else {
                var locationPath = java.nio.file.Path.of(location);
                validateResult = validatePath(locationPath, depositType, validationLevel);
            }

            // this information is lost during the validation, so set it again here
            validateResult.setBagLocation(location);

            return Response.ok(validateResult).build();
        }
        catch (BagNotFoundException e) {
            log.error("Bag not found", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        catch (Exception e) {
            log.error("Internal server error", e);
            return Response.serverError().build();
        }
    }

    @POST
    @Consumes({ "application/zip" })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response validateZip(InputStream inputStream) {
        try {
            var validateResult = validateInputStream(inputStream, DepositType.DEPOSIT, ValidationLevel.STAND_ALONE);
            return Response.ok(validateResult).build();
        }
        catch (BagNotFoundException e) {
            log.error("Bag not found", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        catch (Exception e) {
            log.error("Internal server error", e);
            return Response.serverError().build();
        }
    }

    ValidateOkDto validateInputStream(InputStream inputStream, DepositType depositType, ValidationLevel validationLevel) throws Exception {
        var tempPath = fileService.extractZipFile(inputStream);

        try {
            var bagDir = fileService.getFirstDirectory(tempPath)
                .orElseThrow(() -> new BagNotFoundException("Extracted zip does not contain a directory"));

            return validatePath(bagDir, depositType, validationLevel);
        }
        finally {
            try {
                fileService.deleteDirectoryAndContents(tempPath);
            }
            catch (IOException e) {
                log.error("Error cleaning up temporary directory");
            }
        }

    }

    ValidateOkDto validatePath(java.nio.file.Path bagDir, DepositType depositType, ValidationLevel validationLevel) throws Exception {
        var results = ruleEngineService.validateBag(bagDir, depositType, validationLevel);
        var isValid = results.stream().noneMatch(r -> r.getStatus().equals(RuleValidationResult.RuleValidationResultStatus.FAILURE));

        var result = new ValidateOkDto();
        result.setBagLocation(null);
        result.setIsCompliant(isValid);
        result.setName(bagDir.getFileName().toString());
        result.setProfileVersion("1.0.0");
        result.setInformationPackageType(toInfoPackageType(depositType));
        result.setLevel(toLevel(validationLevel));
        result.setRuleViolations(results.stream()
            .filter(r -> r.getStatus().equals(RuleValidationResult.RuleValidationResultStatus.FAILURE))
            .map(rule -> {
                var ret = new ValidateOkRuleViolationsDto();
                ret.setRule(rule.getNumber());

                var message = new StringBuilder();

                if (rule.getErrorMessage() != null) {
                    message.append(rule.getErrorMessage());
                }

                ret.setViolation(message.toString());
                return ret;
            })
            .collect(Collectors.toList()));

        return result;
    }

    DepositType toDepositType(ValidateCommandDto.PackageTypeEnum value) {
        if (value == ValidateCommandDto.PackageTypeEnum.MIGRATION) {
            return DepositType.MIGRATION;
        }
        return DepositType.DEPOSIT;
    }

    ValidationLevel toValidationLevel(ValidateCommandDto.LevelEnum value) {
        if (value == ValidateCommandDto.LevelEnum.WITH_DATA_STATION_CONTEXT) {
            return ValidationLevel.WITH_DATA_STATION_CONTEXT;
        }
        return ValidationLevel.STAND_ALONE;
    }

    ValidateOkDto.InformationPackageTypeEnum toInfoPackageType(DepositType value) {
        if (value == DepositType.MIGRATION) {
            return ValidateOkDto.InformationPackageTypeEnum.MIGRATION;
        }
        return ValidateOkDto.InformationPackageTypeEnum.DEPOSIT;
    }

    ValidateOkDto.LevelEnum toLevel(ValidationLevel value) {
        if (value == ValidationLevel.WITH_DATA_STATION_CONTEXT) {
            return ValidateOkDto.LevelEnum.WITH_DATA_STATION_CONTEXT;
        }
        return ValidateOkDto.LevelEnum.STAND_ALONE;
    }
}
