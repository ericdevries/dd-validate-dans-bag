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

    private boolean shouldBeSkipped(NumberedRule rule, Map<String, RuleValidationResult> results) {
        if (rule.getDependencies() != null && rule.getDependencies().size() > 0) {
            for (var dependency : rule.getDependencies()) {
                var result = results.get(dependency);

                if (result != null) {
                    // if the parent was skipped or failed, return true
                    if (RuleValidationResult.RuleValidationResultStatus.SKIPPED.equals(result.getStatus()) || RuleValidationResult.RuleValidationResultStatus.FAILURE.equals(result.getStatus())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public List<RuleValidationResult> validateRules(Path bag, NumberedRule[] rules, DepositType depositType) {
        var ruleResults = new HashMap<String, RuleValidationResult>();
        var remainingRules = new ArrayList<>(Arrays.asList(rules));

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
                    try {
                        rule.getRule().validate(bag);
                        ruleResults.put(number, new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.SUCCESS));
                    }
                    catch (RuleSkippedException e) {
                        ruleResults.put(number, new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.SKIPPED));
                    }
                    catch (RuleViolationDetailsException e) {
                        ruleResults.put(number, new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.FAILURE, e));
                    }
                    catch (Throwable e) {
                        ruleResults.put(number, new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.FAILURE, e));
                        log.error("Error happened while executing rule", e);
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
//
//        for (var rule : rules) {
//            var result = ruleResults.get(rule.getNumber());
//            var state = result.getStatus().toString();
//
//            if (result.getStatus().equals(RuleValidationResult.RuleValidationResultStatus.FAILURE)) {
//                System.out.println("RULE [" + rule.getNumber() + "] " + state + ": " + result.getException().getLocalizedMessage());
//            }
//            else {
//                System.out.println("RULE [" + rule.getNumber() + "] " + state);
//            }
//        }

        return new ArrayList<>(ruleResults.values());
    }

    private boolean shouldBeIgnoredBecauseOfDepositType(NumberedRule rule, DepositType depositType) {
        if (DepositType.ALL.equals(rule.getDepositType())) {
            return false;
        }

        return !rule.getDepositType().equals(depositType);
    }
}
