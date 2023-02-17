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

import nl.knaw.dans.validatedansbag.core.rules.BagValidatorRule;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleEngineImplTest {

    @Test
    void validateRules_should_call_all_rules_exactly_once() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.3", fakeRule),
            new NumberedRule("1.4", fakeRule),
        };

        var engine = new RuleEngineImpl();
        assertDoesNotThrow(() -> engine.validateRuleConfiguration(rules));
        assertDoesNotThrow(() -> engine.validateRules(Path.of("somedir"), rules, DepositType.DEPOSIT));

        Mockito.verify(fakeRule, Mockito.times(4)).validate(Mockito.any());
    }

    @Test
    void validateRules_should_skip_task_if_dependency_failed() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var fakeRuleSkipped = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRuleSkipped),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
        };

        var failedResult = new RuleResult(RuleResult.Status.ERROR, List.of());
        Mockito.when(fakeRuleSkipped.validate(Mockito.any())).thenReturn(failedResult);

        var engine = new RuleEngineImpl();
        assertDoesNotThrow(() -> engine.validateRuleConfiguration(rules));
        assertDoesNotThrow(() -> engine.validateRules(Path.of("somedir"), rules, DepositType.DEPOSIT));

        Mockito.verify(fakeRule, Mockito.times(2)).validate(Mockito.any());
        Mockito.verify(fakeRuleSkipped).validate(Mockito.any());
    }

    @Test
    void validateResult_should_skip_rule_with_different_deposit_type() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);

        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule, DepositType.DEPOSIT),
            new NumberedRule("1.2", fakeRule, DepositType.MIGRATION),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
        };

        var engine = new RuleEngineImpl();
        assertDoesNotThrow(() -> engine.validateRuleConfiguration(rules));
        assertDoesNotThrow(() -> engine.validateRules(Path.of("somedir"), rules, DepositType.DEPOSIT));

        Mockito.verify(fakeRule, Mockito.times(4)).validate(Mockito.any());
    }

    @Test
    void validateRuleConfiguration_should_throw_when_duplicate_rules_exist() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule, DepositType.DEPOSIT),
            new NumberedRule("1.2", fakeRule, DepositType.DEPOSIT),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
        };

        var engine = new RuleEngineImpl();

        assertThrows(RuleEngineConfigurationException.class,
            () -> engine.validateRuleConfiguration(rules));

    }

    @Test
    void validateRuleConfiguration_should_throw_when_dependencies_are_missing() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);

        // will fail because 1.2 does not exist and 1.3 depends on it
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
        };

        var engine = new RuleEngineImpl();

        assertThrows(RuleEngineConfigurationException.class,
            () -> engine.validateRuleConfiguration(rules));

    }

    @Test
    void validateRuleConfiguration_should_throw_when_dependency_is_unsatisfied_for_both_depositTypes() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);

        // This will fail because 1.3 depends on 1.2, but 1.2 is of type DEPOSIT
        // while 1.3 is applicable to both deposit types.
        // If the deposit were of type MIGRATION, this would create unresolved dependencies
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule, DepositType.DEPOSIT),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
        };

        var engine = new RuleEngineImpl();

        assertThrows(RuleEngineConfigurationException.class,
            () -> engine.validateRuleConfiguration(rules));

    }

    @Test
    void validateRuleConfiguration_should_throw_when_rules_are_duplicated() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);

        // This will fail because 1.2 is declared as both a "generic" rule type
        // and a MIGRATION type. It should either be declared with 2 explicit
        // DepositType's, or just once as a generic rule.
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.2", fakeRule, DepositType.MIGRATION),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
        };

        var engine = new RuleEngineImpl();

        assertThrows(RuleEngineConfigurationException.class,
            () -> engine.validateRuleConfiguration(rules));

    }

    @Test
    void validateRuleConfiguration_should_not_throw_when_duplicate_rules_have_different_types() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);

        // Will not throw because 1.3 is declared with explicit types which do not overlap
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.3", fakeRule, DepositType.DEPOSIT, List.of("1.2")),
            new NumberedRule("1.3", fakeRule, DepositType.MIGRATION, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
        };

        var engine = new RuleEngineImpl();

        assertDoesNotThrow(
            () -> engine.validateRuleConfiguration(rules));

    }

    @Test
    void validateRuleConfiguration_should_not_throw_with_multiple_duplicate_rules() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);

        // This emulates a chain of dependencies with explicit DepositType's, which is valid
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.3", fakeRule, DepositType.DEPOSIT, List.of("1.2")),
            new NumberedRule("1.3", fakeRule, DepositType.MIGRATION, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
            new NumberedRule("1.6", fakeRule, DepositType.DEPOSIT, List.of("1.3")),
            new NumberedRule("1.6", fakeRule, DepositType.MIGRATION, List.of("1.3")),
        };

        var engine = new RuleEngineImpl();

        assertDoesNotThrow(
            () -> engine.validateRuleConfiguration(rules));

    }

    @Test
    void validateRules_should_return_exactly_3_results_when_rule_is_violated() throws Exception {
        var badResult = new RuleResult(RuleResult.Status.ERROR, List.of());
        var goodResult = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        var fakeRule = Mockito.mock(BagValidatorRule.class);

        var fakeErrorRule = Mockito.mock(BagValidatorRule.class);
        Mockito.when(fakeErrorRule.validate(Mockito.any())).thenReturn(badResult);
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(goodResult);

        // This caused issues as described in DD-1135
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.3", fakeErrorRule, DepositType.DEPOSIT, List.of("1.2")),
            new NumberedRule("1.3", fakeErrorRule, DepositType.MIGRATION, List.of("1.2")),
        };

        var engine = new RuleEngineImpl();
        var result = engine.validateRules(Path.of("bagdir"), rules, DepositType.DEPOSIT);

        assertEquals(3, result.size());
    }
}