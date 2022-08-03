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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RuleEngineImpl implements RuleEngine {
    @Override
    public List<RuleValidationResult> validateRules(Path bag, NumberedRule[] rules) {
        var ruleResults = new HashMap<String, RuleValidationResult>();

        for (var rule : rules) {
            var executeRule = true;
            var number = rule.getNumber();

            if (rule.getDependencies() != null) {
                // check all dependencies
                for (var dependency : rule.getDependencies()) {
                    if (!RuleValidationResult.RuleValidationResultStatus.SUCCESS.equals(ruleResults.get(dependency).getStatus())) {
                        ruleResults.put(number, new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.SKIPPED));
                        executeRule = false;
                        break;
                    }
                }
            }

            if (executeRule) {
                try {
                    rule.getRule().validate(bag);
                    ruleResults.put(number, new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.SUCCESS));
                }
                catch (Throwable e) {
                    ruleResults.put(number, new RuleValidationResult(number, RuleValidationResult.RuleValidationResultStatus.FAILURE, e));
                }
            }
        }

        for (var rule : rules) {
            var result = ruleResults.get(rule.getNumber());
            var state = result.getStatus().toString();

            if (result.getStatus().equals(RuleValidationResult.RuleValidationResultStatus.FAILURE)) {

                System.out.println("RULE [" + rule.getNumber() + "] " + state + ": " + result.getException().getLocalizedMessage());
            } else {
                System.out.println("RULE [" + rule.getNumber() + "] " + state);
            }
        }

        return new ArrayList<>(ruleResults.values());
    }
}
