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

import nl.knaw.dans.validatedansbag.core.engine.RuleResult;
import nl.knaw.dans.validatedansbag.core.service.XmlReaderImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetXmlContainsExactlyOneDctermsLicenseWithXsiTypeUriTest extends RuleTestFixture {

    @Test
    void should_return_ERROR_for_zero_licenses() throws Exception {
        final String xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:dcmiMetadata>\n"
                + "    </ddm:dcmiMetadata>\n"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlContainsExactlyOneDctermsLicenseWithXsiTypeUri(reader, licenseValidator).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_ERROR_for_multiple_licenses() throws Exception {
        final String xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:dcmiMetadata>\n"
                + "        <dcterms:license xsi:type=\"dcterms:URI\">http://opensource.org/licenses/MIT</dcterms:license>\n"
                + "        <dcterms:license xsi:type=\"dcterms:URI\">http://opensource.org/licenses/MIT</dcterms:license>\n"
                + "    </ddm:dcmiMetadata>\n"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlContainsExactlyOneDctermsLicenseWithXsiTypeUri(reader, licenseValidator).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_SUCCESS_for_valid_URI() throws Exception {
        final String xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:dcmiMetadata>\n"
                + "        <dcterms:license xsi:type=\"dcterms:URI\">http://random.org/licenses/MIT</dcterms:license>\n"
                + "    </ddm:dcmiMetadata>\n"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlContainsExactlyOneDctermsLicenseWithXsiTypeUri(reader, licenseValidator).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }


    @Test
    void should_return_SUCCESS_for_valid_URI_and_extra_license_without_attribute() throws Exception {
        final String xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:dcmiMetadata>\n"
                + "        <dcterms:license xsi:type=\"dcterms:URI\">http://random.org/licenses/MIT</dcterms:license>\n"
                + "        <dcterms:license>NOT MARKED WITH XSI TYPE, SO SHOULD BE IGNORED</dcterms:license>\n"
                + "    </ddm:dcmiMetadata>\n"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlContainsExactlyOneDctermsLicenseWithXsiTypeUri(reader, licenseValidator).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }



    @Test
    void should_return_ERROR_for_nondcterms_uri() throws Exception {
        final String xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:dcmiMetadata>\n"
                + "        <dcterms:license xsi:type=\"dcterms:SOMETHING_ELSE\">http://random.org/licenses/MIT</dcterms:license>\n"
                + "    </ddm:dcmiMetadata>\n"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlContainsExactlyOneDctermsLicenseWithXsiTypeUri(reader, licenseValidator).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

    @Test
    void should_return_ERROR_for_invalid_uri() throws Exception {
        final String xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:dcmiMetadata>\n"
                + "        <dcterms:license xsi:type=\"dcterms:URI\">invalid uri</dcterms:license>\n"
                + "    </ddm:dcmiMetadata>\n"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlContainsExactlyOneDctermsLicenseWithXsiTypeUri(reader, licenseValidator).validate(Path.of("bagdir"));

        assertEquals(RuleResult.Status.ERROR, result.getStatus());
    }

}
