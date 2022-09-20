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
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleEngineImplTest {

    @Test
    void testRules() throws Exception {
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
        assertDoesNotThrow(() -> engine.validateRules(Path.of("somedir"), rules, DepositType.DEPOSIT, ValidationLevel.STAND_ALONE));

        Mockito.verify(fakeRule, Mockito.times(4)).validate(Mockito.any());
    }

    @Test
    void testRulesButOneIsSkipped() throws Exception {
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
        assertDoesNotThrow(() -> engine.validateRules(Path.of("somedir"), rules, DepositType.DEPOSIT, ValidationLevel.STAND_ALONE));

        Mockito.verify(fakeRule, Mockito.times(2)).validate(Mockito.any());
        Mockito.verify(fakeRuleSkipped).validate(Mockito.any());
    }

    @Test
    void testRulesButOneHasFailed() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var fakeRuleFailed = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRuleFailed),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
        };

        var failedResult = new RuleResult(RuleResult.Status.ERROR, List.of());
        Mockito.doReturn(failedResult).when(fakeRuleFailed).validate(Mockito.any());

        var engine = new RuleEngineImpl();
        assertDoesNotThrow(() -> engine.validateRuleConfiguration(rules));
        assertDoesNotThrow(() -> engine.validateRules(Path.of("somedir"), rules, DepositType.DEPOSIT, ValidationLevel.STAND_ALONE));

        Mockito.verify(fakeRule, Mockito.times(2)).validate(Mockito.any());
        Mockito.verify(fakeRuleFailed).validate(Mockito.any());
    }

    @Test
    void testRulesWithTwoDepositTypes() throws Exception {
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
        assertDoesNotThrow(() -> engine.validateRules(Path.of("somedir"), rules, DepositType.DEPOSIT, ValidationLevel.STAND_ALONE));

        Mockito.verify(fakeRule, Mockito.times(4)).validate(Mockito.any());
    }

    @Test
    void testRulesWithDuplicateNumbers() throws Exception {
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
    void testRulesWithMissingDependencies() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
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
    void testRulesWithMissingDependenciesDueToDepositTypes() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
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
    void testRulesWithTooManyRules() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
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
    void testRulesWithDifferentDepositTypesDependOnAllRule() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
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
    void testRulesWithDifferentDepositTypesDependOnAllRuleTwoLevels() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
        var rules = new NumberedRule[] {
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.3", fakeRule, DepositType.DEPOSIT, List.of("1.2")),
            new NumberedRule("1.3", fakeRule, DepositType.MIGRATION, List.of("1.2")),
            new NumberedRule("1.4", fakeRule),
            new NumberedRule("1.6", fakeRule,DepositType.DEPOSIT, List.of("1.3")),
            new NumberedRule("1.6", fakeRule, DepositType.MIGRATION, List.of("1.3")),
        };

        var engine = new RuleEngineImpl();

        assertDoesNotThrow(
            () -> engine.validateRuleConfiguration(rules));

    }
}