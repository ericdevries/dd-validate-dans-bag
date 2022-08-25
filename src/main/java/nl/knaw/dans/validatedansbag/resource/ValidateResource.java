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
import nl.knaw.dans.openapi.api.ValidateJsonOkDto;
import nl.knaw.dans.openapi.api.ValidateJsonOkRuleViolationsDto;
import nl.knaw.dans.validatedansbag.core.engine.DepositType;
import nl.knaw.dans.validatedansbag.core.engine.RuleValidationResult;
import nl.knaw.dans.validatedansbag.core.service.FileService;
import nl.knaw.dans.validatedansbag.core.service.RuleEngineService;
import org.glassfish.jersey.media.multipart.FormDataParam;

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
import java.util.List;
import java.util.stream.Collectors;

@Path("/validate")
public class ValidateResource {

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

        try {
            ValidateResult validateResult;

            if (location == null) {
                validateResult = validateInputStream(zipInputStream, depositType);
            }
            else {
                validateResult = validatePath(location, depositType);
            }

            var result = new ValidateJsonOkDto();
            result.setBagLocation(location);
            result.setIsCompliant(validateResult.isValid());
            result.setName(validateResult.getBagName());
            result.setProfileVersion("1.0.0");
            result.setInfoPackageType(toInfoPackageType(depositType));
            result.setRuleViolations(validateResult.getResults().stream()
                .filter(r -> r.getStatus().equals(RuleValidationResult.RuleValidationResultStatus.FAILURE))
                .map(rule -> {
                    var ret = new ValidateJsonOkRuleViolationsDto();
                    ret.setRule(rule.getNumber());
                    ret.setViolation(rule.getException().getLocalizedMessage());
                    return ret;
                })
                .collect(Collectors.toList()));

            return Response.ok(result).build();
        }
        catch (Throwable e) {
            e.printStackTrace();
        }

        return Response.ok().build();
    }

    @POST
    @Consumes({ "application/zip" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateZip(InputStream inputStream) {
        try {
            var validateResult = validateInputStream(inputStream, DepositType.DEPOSIT);

            var result = new ValidateJsonOkDto();
            result.setBagLocation(null);
            result.setIsCompliant(validateResult.isValid());
            result.setName(validateResult.getBagName());
            result.setProfileVersion("1.0.0");
            result.setInfoPackageType(toInfoPackageType(DepositType.DEPOSIT));
            result.setRuleViolations(validateResult.getResults().stream()
                .filter(r -> r.getStatus().equals(RuleValidationResult.RuleValidationResultStatus.FAILURE))
                .map(rule -> {
                    var ret = new ValidateJsonOkRuleViolationsDto();
                    ret.setRule(rule.getNumber());
                    ret.setViolation(rule.getException().getLocalizedMessage());
                    return ret;
                })
                .collect(Collectors.toList()));

            return Response.ok(result).build();
        }
        catch (IOException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    ValidateResult validateInputStream(InputStream inputStream, DepositType depositType) throws IOException {
        var bagDir = fileService.extractZipFile(inputStream)
            .orElseThrow(() -> new IOException("Extracted zip does not contain a directory"));

        var results = ruleEngineService.validateBag(bagDir, depositType);

        return new ValidateResult(bagDir.getFileName().toString(), results);
    }

    ValidateResult validatePath(String location, DepositType depositType) throws IOException {
        var bagDir = java.nio.file.Path.of(location);
        var results = ruleEngineService.validateBag(bagDir, depositType);
        return new ValidateResult(bagDir.getFileName().toString(), results);
    }

    DepositType toDepositType(ValidateCommandDto.PackageTypeEnum value) {
        switch (value) {
            case MIGRATION:
                return DepositType.MIGRATION;
            default:
                return DepositType.DEPOSIT;
        }
    }

    ValidateJsonOkDto.InfoPackageTypeEnum toInfoPackageType(DepositType value) {
        switch (value) {
            case MIGRATION:
                return ValidateJsonOkDto.InfoPackageTypeEnum.MIGRATION;
            default:
                return ValidateJsonOkDto.InfoPackageTypeEnum.DEPOSIT;
        }
    }

    static class ValidateResult {
        private boolean isValid;
        private String bagName;
        private List<RuleValidationResult> results;

        public ValidateResult(String bagName, List<RuleValidationResult> results) {
            this.bagName = bagName;
            this.results = results;
            this.isValid = this.results.stream().noneMatch(r -> r.getStatus().equals(RuleValidationResult.RuleValidationResultStatus.FAILURE));
        }

        public boolean isValid() {
            return isValid;
        }

        public String getBagName() {
            return bagName;
        }

        public List<RuleValidationResult> getResults() {
            return results;
        }
    }
}
