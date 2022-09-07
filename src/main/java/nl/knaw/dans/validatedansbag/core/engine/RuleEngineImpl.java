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
package nl.knaw.dans.validatedansbag.core.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuleEngineImpl implements RuleEngine {
    private static final Logger log = LoggerFactory.getLogger(RuleEngineImpl.class);

    // returns true if all dependencies are marked as SUCCESS
    private boolean canBeExecuted(NumberedRule rule, Map<String, RuleValidationResult> results) {
        if (rule.getDependencies() != null && rule.getDependencies().size() > 0) {
            for (var dependency : rule.getDependencies()) {
                var result = results.get(dependency);

                // if the parent was skipped or failed, return true
                if (result == null || RuleValidationResult.RuleValidationResultStatus.SKIPPED.equals(result.getStatus()) || RuleValidationResult.RuleValidationResultStatus.FAILURE.equals(
                    result.getStatus())) {
                    return false;
                }
            }
        }

        return true;
    }

    // return true if one of its dependencies was skipped, or it has failed
    private boolean shouldBeSkipped(NumberedRule rule, Map<String, RuleValidationResult> results) {
        if (rule.getDependencies() != null && rule.getDependencies().size() > 0) {
            for (var dependency : rule.getDependencies()) {
                var result = results.get(dependency);

                if (result != null) {
                    // if the parent was skipped or failed, return true
                    if (RuleValidationResult.RuleValidationResultStatus.SKIPPED.equals(result.getStatus()) || RuleValidationResult.RuleValidationResultStatus.FAILURE.equals(result.getStatus())
                        || result.isShouldSkipDependencies()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public List<RuleValidationResult> validateRules(Path bag, NumberedRule[] rules, DepositType depositType) throws Exception {
        // validate each rule number is unique
        var duplicateRules = getDuplicateRules(rules);

        if (!duplicateRules.isEmpty()) {
            throw new RuleEngineConfigurationException(String.format(
                "Duplicate rule numbers found: %s", String.join(", ", duplicateRules)
            ));
        }

        // make sure each rule depends on an existing rule
        var unresolvedDependencies = getUnresolvedDependencies(rules);

        if (!unresolvedDependencies.isEmpty()) {
            throw new RuleEngineConfigurationException(String.format(
                "Some rules depend on other rules that do not exist: %s", String.join(", ", unresolvedDependencies)
            ));
        }

        return doValidateRules(bag, rules, depositType);
    }

    public List<RuleValidationResult> doValidateRules(Path bag, NumberedRule[] rules, DepositType depositType) throws Exception {
        var ruleResults = new HashMap<String, RuleValidationResult>();
        var remainingRules = filterRulesOnDepositType(rules, depositType);

        while (remainingRules.size() > 0) {
            var toRemove = new HashSet<NumberedRule>();

            for (var rule : remainingRules) {
                var number = rule.getNumber();

                // will never be processed, so skip it and remove it from the remaining rules
                if (shouldBeSkipped(rule, ruleResults)) {
                    log.trace("Skipping task {} because dependencies are not successful", rule.getNumber());
                    ruleResults.put(number, new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.SKIPPED));
                    toRemove.add(rule);
                }
                else if (shouldBeIgnoredBecauseOfDepositType(rule, depositType)) {
                    log.trace("Skipping task {} because it does not apply to this deposit (deposit type: {}, rule type: {})", rule.getNumber(), depositType, rule.getDepositType());
                    ruleResults.put(number, new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.SKIPPED));
                    toRemove.add(rule);
                }
                else if (canBeExecuted(rule, ruleResults)) {
                    log.trace("Executing task {}", rule.getNumber());
                    var response = rule.getRule().validate(bag);

                    log.trace("Task result: {}", response.getStatus());
                    RuleValidationResult ruleValidationResult = null;

                    switch (response.getStatus()) {
                        case SUCCESS:
                            ruleValidationResult = new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.SUCCESS);
                            break;
                        case SKIP_DEPENDENCIES:
                            ruleValidationResult = new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.SUCCESS, true);
                            break;
                        case ERROR:
                            ruleValidationResult = new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.FAILURE, formatErrorMessages(response.getErrorMessages()));
                            break;
                    }

                    ruleResults.put(number, ruleValidationResult);

                    if (response.getException() != null) {
                        log.warn("Rule provided an exception while executing", response.getException());
                    }

                    toRemove.add(rule);
                }
                else {
                    log.trace("Skipping rule {} because its dependencies have not yet executed", rule);
                }
            }

            remainingRules.removeAll(toRemove);

            if (toRemove.size() == 0) {
                log.warn("No rules executed this round, but there are still rules to be checked; most likely a dependency configuration error!");

                for (var rule : remainingRules) {
                    log.warn(" - Rule {} is yet to be executed", rule);
                }

                break;
            }
        }

        // TODO this does not belong here, but it would be nice to log the results of the validation
        reportOnBag(rules, ruleResults);

        return Stream.of(rules)
            .map(rule -> ruleResults.get(rule.getNumber()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private String padLeft(String s, int amount) {
        return String.format("%" + amount + "s", s);
    }

    private void reportOnBag(NumberedRule[] rules, Map<String, RuleValidationResult> ruleResults) {
        var maxRuleLength = Stream.of(rules)
            .map(r -> r.getNumber().length())
            .max(Integer::compare)
            .orElse(0);

        var resultsAsString = Stream.of(rules)
            .map(rule -> {
                var result = ruleResults.get(rule.getNumber());
                var resultStatus = result == null ? RuleValidationResult.RuleValidationResultStatus.SKIPPED : result.getStatus();
                var padding = maxRuleLength - rule.getNumber().length() + 1 + resultStatus.toString().length();

                if (resultStatus.equals(RuleValidationResult.RuleValidationResultStatus.FAILURE)) {
                    return String.format("! Rule %s: %s - %s",
                        rule.getNumber(), padLeft(resultStatus.toString(), padding), result.getErrorMessage());
                }
                else {
                    return String.format("! Rule %s: %s",
                        rule.getNumber(), padLeft(resultStatus.toString(), padding));
                }

            })
            .map(s -> s.replaceAll("\n", "\n!"))
            .collect(Collectors.joining("\n"));

        log.info("Bag validation report: \n{}", resultsAsString);
    }

    private String formatErrorMessages(List<String> errorMessages) {
        if (errorMessages.size() == 1) {
            return errorMessages.get(0);
        }

        return String.join("\n", errorMessages);
    }

    // find any rule that depends on a rule that doesn't exist
    private List<String> getUnresolvedDependencies(NumberedRule[] rules) {
        var unresolved = new ArrayList<String>();

        for (var depositType : List.of(DepositType.DEPOSIT, DepositType.MIGRATION)) {
            var typedRules = filterRulesOnDepositType(rules, depositType);

            var keys = typedRules.stream()
                .map(NumberedRule::getNumber)
                .collect(Collectors.toSet());

            // this does not check for circular dependencies or self-references
            for (var rule : typedRules) {
                if (rule.getDependencies() != null && !keys.containsAll(rule.getDependencies())) {
                    unresolved.add(rule.getNumber());
                }
            }
        }

        return unresolved;
    }

    // find any rule that has a number that is present multiple times in the list
    private List<String> getDuplicateRules(NumberedRule[] rules) {
        var duplicates = new ArrayList<String>();
        var seen = new HashMap<String, DepositType>();

        for (var rule : rules) {
            var number = rule.getNumber();

            // it is considered a duplicate if
            // - one of the 2 (or both) rules have type ALL
            // - both have the same type
            if (seen.containsKey(number)) {
                var s = seen.get(number);

                if (s.equals(DepositType.ALL) || rule.getDepositType().equals(DepositType.ALL)) {
                    duplicates.add(number);
                }

                else if (s.equals(rule.getDepositType())) {
                    duplicates.add(number);
                }
            }

            seen.put(number, rule.getDepositType());
        }

        return duplicates;
    }

    private boolean shouldBeIgnoredBecauseOfDepositType(NumberedRule rule, DepositType depositType) {
        if (DepositType.ALL.equals(rule.getDepositType())) {
            return false;
        }

        return !rule.getDepositType().equals(depositType);
    }

    List<NumberedRule> filterRulesOnDepositType(NumberedRule[] rules, DepositType depositType) {

        return Arrays.stream(rules)
            .filter(rule -> !shouldBeIgnoredBecauseOfDepositType(rule, depositType))
            .collect(Collectors.toList());
    }
}
