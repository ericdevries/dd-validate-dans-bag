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

import static nl.knaw.dans.validatedansbag.core.engine.NumberedRule.numberedRule;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleEngineImplTest {

    @Test
    void testRules() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);

        var rules = new NumberedRule[] {
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRule).build(),
            numberedRule("1.3", fakeRule).build(),
            numberedRule("1.4", fakeRule).build(),
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
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRuleSkipped).build(),
            numberedRule("1.3", fakeRule, "1.2").build(),
            numberedRule("1.4", fakeRule).build(),
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
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRuleFailed).build(),
            numberedRule("1.3", fakeRule, "1.2").build(),
            numberedRule("1.4", fakeRule).build(),
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
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRule).withDepositType(DepositType.DEPOSIT).build(),
            numberedRule("1.2", fakeRule).withDepositType(DepositType.MIGRATION).build(),
            numberedRule("1.3", fakeRule, "1.2").build(),
            numberedRule("1.4", fakeRule).build(),
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
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRule).withDepositType(DepositType.DEPOSIT).build(),
            numberedRule("1.2", fakeRule).withDepositType(DepositType.DEPOSIT).build(),
            numberedRule("1.3", fakeRule, "1.2").build(),
            numberedRule("1.4", fakeRule).build(),
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
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.3", fakeRule, "1.2").build(),
            numberedRule("1.4", fakeRule).build(),
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
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRule).withDepositType(DepositType.DEPOSIT).build(),
            numberedRule("1.3", fakeRule, "1.2").build(),
            numberedRule("1.4", fakeRule).build(),
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
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRule).build(),
            numberedRule("1.2", fakeRule).withDepositType(DepositType.MIGRATION).build(),
            numberedRule("1.3", fakeRule, "1.2").build(),
            numberedRule("1.4", fakeRule).build(),
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
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRule).build(),
            numberedRule("1.3", fakeRule, "1.2").withDepositType(DepositType.DEPOSIT).build(),
            numberedRule("1.3", fakeRule, "1.2").withDepositType(DepositType.MIGRATION).build(),
            numberedRule("1.4", fakeRule).build(),
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
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRule).build(),
            numberedRule("1.3", fakeRule, "1.2").withDepositType(DepositType.DEPOSIT).build(),
            numberedRule("1.3", fakeRule, "1.2").withDepositType(DepositType.MIGRATION).build(),
            numberedRule("1.4", fakeRule).build(),
            numberedRule("1.6", fakeRule, "1.3").withDepositType(DepositType.DEPOSIT).build(),
            numberedRule("1.6", fakeRule, "1.3").withDepositType(DepositType.MIGRATION).build(),
        };

        var engine = new RuleEngineImpl();

        assertDoesNotThrow(
            () -> engine.validateRuleConfiguration(rules));

    }

    @Test
    void testDifferentDepositTypesDontMakeForDuplicateResults() throws Exception {
        var badResult = new RuleResult(RuleResult.Status.ERROR, List.of());
        var goodResult = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        var fakeRule = Mockito.mock(BagValidatorRule.class);

        var fakeErrorRule = Mockito.mock(BagValidatorRule.class);
        Mockito.when(fakeErrorRule.validate(Mockito.any())).thenReturn(badResult);
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(goodResult);

        var rules = new NumberedRule[] {
            numberedRule("1.1", fakeRule).build(),
            numberedRule("1.2", fakeRule).build(),
            numberedRule("1.3", fakeErrorRule, "1.2").withDepositType(DepositType.DEPOSIT).build(),
            numberedRule("1.3", fakeErrorRule, "1.2").withDepositType(DepositType.MIGRATION).build()
        };

        var engine = new RuleEngineImpl();
        var result = engine.validateRules(Path.of("bagdir"), rules, DepositType.DEPOSIT, ValidationLevel.STAND_ALONE);

        assertEquals(3, result.size());
    }
}