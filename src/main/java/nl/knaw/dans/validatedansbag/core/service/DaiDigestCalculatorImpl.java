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

public class DaiDigestCalculatorImpl implements DaiDigestCalculator {
    @Override
    public char calculateChecksum(String str, int modeMax) {
        var reversed = new StringBuilder(str).reverse().toString();
        var f = 2;
        var w = 0;
        var mod = 0;

        while (mod < reversed.length()) {
            var cx = reversed.charAt(mod);
            var x = cx - 48;
            w += f * x;
            f += 1;

            if (f > modeMax) {
                f = 2;
            }

            mod += 1;
        }

        mod = w % 11;

        if (mod == 0) {
            return '0';
        }

        else {
            var c = 11 - mod;

            if (c == 10) {
                return 'X';
            }
            else {
                return (char) (c + 48);
            }
        }
    }
}
