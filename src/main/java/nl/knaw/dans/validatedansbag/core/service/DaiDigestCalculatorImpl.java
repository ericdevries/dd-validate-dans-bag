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
package nl.knaw.dans.validatedansbag.core.service;

import java.util.concurrent.atomic.AtomicInteger;

public class DaiDigestCalculatorImpl implements DaiDigestCalculator {
    /**
     * For details about the way this works, see https://en.wikipedia.org/wiki/MSI_Barcode#Mod_11_Check_Digit
     *
     * @param str
     * @return the checksum
     */
    @Override
    public char calculateChecksum(String str) {
        var index = new AtomicInteger();

        // iterate each digit in a reversed string
        // note that the weights are a sequence based on index starting
        // from 2 up to and including 9 and once it goes over the max it
        // resets to the first item in the sequence
        // for example: 2,3,4,5,6,7,8,9,2,3,4 etc
        var sum = new StringBuilder(str).reverse().toString()
            .chars()
            // convert digit character to numerical value
            .map(c -> c - 48)
            // multiply digit by weight
            .map((c -> ((index.getAndIncrement() % 8) + 2) * c))
            .sum();

        // apply this calculation to the sum of the digits multiplied by weights
        var check = (11 - (sum % 11)) % 11;

        // convert numerical value back to character
        return check == 10 ? 'X' : (char)(check + 48);
    }
}
