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
package nl.knaw.dans.validatedansbag.core.validator;

import nl.knaw.dans.validatedansbag.core.validator.IdentifierValidator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IdentifierValidatorImpl implements IdentifierValidator {
    private final String daiPrefix = "info:eu-repo/dai/nl/";
    private final String orcidPrefix = "https://orcid.org/";
    private final String isniPrefix = "https://isni.org/isni/";

    private final List<String> orcidDomains = List.of("orcid.org", "www.orcid.org");
    private final List<String> isniDomains = List.of("isni.org", "www.isni.org");

    /**
     * For details about the way this works, see https://en.wikipedia.org/wiki/MSI_Barcode#Mod_11_Check_Digit
     *
     * @param str
     * @return the checksum
     */
    @Override
    public boolean validateDai(String str) {
        str = str.replaceAll(daiPrefix, "");

        var index = new AtomicInteger();
        var actual = str.charAt(str.length() - 1);

        // iterate each digit in a reversed string
        // note that the weights are a sequence based on index starting
        // from 2 up to and including 9 and once it goes over the max it
        // resets to the first item in the sequence
        // for example: 2,3,4,5,6,7,8,9,2,3,4 etc
        var sum = new StringBuilder(str).reverse().substring(1)
            .chars()
            // convert digit character to numerical value
            .map(c -> c - 48)
            // multiply digit by weight
            .map((c -> ((index.getAndIncrement() % 8) + 2) * c))
            .sum();

        // apply this calculation to the sum of the digits multiplied by weights
        var check = (11 - (sum % 11)) % 11;
        // convert numerical value back to character
        var expected = check == 10 ? 'X' : (char) (check + 48);

        return expected == actual;
    }

    /**
     * Information about the ORCID ID: https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier
     *
     * @param str
     * @return
     */
    @Override
    public boolean validateOrcid(String str) {
        try {
            // strip both https and http, although the specs state it should only have https
            // see: https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier
            // - The ORCID iD is expressed as an https URI, i.e. the 16-digit identifier is preceded by "https://orcid.org/". A hyphen is inserted every 4 digits of the identifier to aid readability.
            var uri = new URI(str);

            if (uri.getHost() != null) {
                // domain should be in the list
                if (!orcidDomains.contains(uri.getHost())) {
                    return false;
                }

                str = uri.getPath().substring(1);
            }
        }
        catch (URISyntaxException e) {
            // it is not a uri, but that is not a problem
        }

        str = str.replaceAll("-", "");

        return validateMod11Two(str);
    }

    @Override
    public boolean validateIsni(String str) {
        try {
            var uri = new URI(str);

            if (uri.getHost() != null) {
                // domain should be in the list
                if (!isniDomains.contains(uri.getHost())) {
                    return false;
                }

                str = uri.getPath().replaceFirst("/isni/", "");
            }
        }
        catch (URISyntaxException e) {
            // it is not a uri, but that is not a problem
        }

        str = str.replaceAll("[\\s-]", "");

        return validateMod11Two(str);
    }

    boolean validateMod11Two(String str) {
        if (str.length() != 16) {
            return false;
        }

        var actual = str.charAt(str.length() - 1);
        var sum = new StringBuilder(str).substring(0, str.length() - 1)
            .chars()
            // convert digit character to numerical value
            .map(c -> c - 48)
            // add the result to the previous result and multiply it by 2
            .reduce(0, (i1, i2) -> (i1 + i2) * 2);

        // apply this calculation to the total
        var check = (12 - (sum % 11)) % 11;

        // convert numerical value back to character
        var expected = check == 10 ? 'X' : (char) (check + 48);

        return expected == actual;
    }
}
