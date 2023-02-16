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

import java.util.List;

public class NumberedRule {
    private final String number;
    private final BagValidatorRule rule;
    private final List<String> dependencies;
    private final DepositType depositType;

    public NumberedRule(String number, BagValidatorRule rule, DepositType depositType, List<String> dependencies) {
        this.number = number;
        this.rule = rule;
        this.dependencies = dependencies;
        this.depositType = depositType;
    }

    public NumberedRule(String number, BagValidatorRule rule) {
        this(number, rule, null, null);
    }

    public NumberedRule(String number, BagValidatorRule rule, List<String> dependencies) {
        this(number, rule, null, dependencies);
    }

    public NumberedRule(String number, BagValidatorRule rule, DepositType depositType) {
        this(number, rule, depositType, null);
    }

    public DepositType getDepositType() {
        return depositType;
    }

    public String getNumber() {
        return number;
    }

    public BagValidatorRule getRule() {
        return rule;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        return "NumberedRule{" +
            "number='" + number + '\'' +
            ", rule=" + rule +
            ", dependencies=" + dependencies +
            ", depositType=" + depositType +
            '}';
    }

}
