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
package nl.knaw.dans.validatedansbag.core.rules;

import nl.knaw.dans.validatedansbag.core.config.LicenseConfig;

import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestLicenseConfig extends LicenseConfig {

    public TestLicenseConfig() {
        super();

        var licences = Stream.of(
                "http://creativecommons.org/licenses/by-nc-nd/4.0/",
                "http://creativecommons.org/licenses/by-nc-sa/3.0",
                "http://creativecommons.org/licenses/by-nc-sa/4.0/",
                "http://creativecommons.org/licenses/by-nc/3.0",
                "http://creativecommons.org/licenses/by-nc/4.0/",
                "http://creativecommons.org/licenses/by-nd/4.0/",
                "http://creativecommons.org/licenses/by-sa/4.0/",
                "http://creativecommons.org/licenses/by/4.0",
                "http://creativecommons.org/publicdomain/zero/1.0",
                "http://opendatacommons.org/licenses/by/1-0/index.html",
                "http://opensource.org/licenses/BSD-2-Clause",
                "http://opensource.org/licenses/BSD-3-Clause",
                "http://opensource.org/licenses/MIT",
                "http://www.apache.org/licenses/LICENSE-2.0",
                "http://www.cecill.info/licences/Licence_CeCILL-B_V1-en.html",
                "http://www.cecill.info/licences/Licence_CeCILL_V2-en.html",
                "http://www.gnu.org/licenses/gpl-3.0.en.html",
                "http://www.gnu.org/licenses/lgpl-3.0.txt",
                "http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html",
                "http://www.mozilla.org/en-US/MPL/2.0/FAQ/",
                "http://www.ohwr.org/attachments/2388/cern_ohl_v_1_2.txt",
                "http://www.ohwr.org/attachments/735/CERNOHLv1_1.txt",
                "http://www.ohwr.org/projects/cernohl/wiki",
                "http://www.tapr.org/TAPR_Open_Hardware_License_v1.0.txt",
                "http://www.tapr.org/ohl.html",
                "http://dans.knaw.nl/en/about/organisation-and-policy/legal-information/DANSGeneralconditionsofuseUKDEF.pdf",
                "http://dans.knaw.nl/en/about/organisation-and-policy/legal-information/DANSLicence.pdf"
            )
            .map(URI::create)
            .collect(Collectors.toList());

        setAllowedLicenses(licences);
    }
}
