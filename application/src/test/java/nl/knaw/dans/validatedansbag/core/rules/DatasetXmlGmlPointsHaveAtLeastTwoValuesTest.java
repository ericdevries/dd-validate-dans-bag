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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DatasetXmlGmlPointsHaveAtLeastTwoValuesTest extends RuleTestFixture {

    @Test
    void should_return_ERROR_if_only_value_or_RD_coordinates_outside_bounds() throws Exception {
        var xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dct=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:dcx-gml=\"http://easy.dans.knaw.nl/schemas/dcx/gml/\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:dcmiMetadata>\n"
                + "            <dct:spatial xsi:type='dcx-gml:SimpleGMLType'>"
                + "                <Point xmlns='http://www.opengis.net/gml'>"
                + "                    <pos>1.0</pos><!-- only one value -->"
                + "                </Point>"
                + "            </dct:spatial>"
                + "            <dct:spatial xsi:type='dcx-gml:SimpleGMLType'>"
                + "                <Point xmlns='http://www.opengis.net/gml'>"
                + "                    <pos>a 5</pos><!-- non numeric -->"
                + "                </Point>"
                + "            </dct:spatial>"
                + "            <dcx-gml:spatial>"
                + "                <boundedBy xmlns='http://www.opengis.net/gml'>"
                + "                    <Envelope srsName='urn:ogc:def:crs:EPSG::28992'>"
                + "                        <lowerCorner>-7001 289000</lowerCorner>"
                + "                        <upperCorner>300001 629000</upperCorner>"
                + "                    </Envelope>"
                + "                </boundedBy>"
                + "            </dcx-gml:spatial>"
                + "            <dcx-gml:spatial>"
                + "                <boundedBy xmlns='http://www.opengis.net/gml'>"
                + "                    <Envelope srsName='urn:ogc:def:crs:EPSG::28992'>"
                + "                        <lowerCorner>-7000 288999</lowerCorner>"
                + "                        <upperCorner>300000 629001</upperCorner>"
                + "                    </Envelope>"
                + "                </boundedBy>"
                + "            </dcx-gml:spatial>"
                + "            <dcx-gml:spatial>"
                + "                <boundedBy xmlns='http://www.opengis.net/gml'>"
                + "                    <Envelope><!-- no srsName -->"
                + "                        <lowerCorner>-7000</lowerCorner>"
                + "                    </Envelope>"
                + "                </boundedBy>"
                + "            </dcx-gml:spatial>"
                + "    </ddm:dcmiMetadata>"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlGmlPointsHaveAtLeastTwoValues(reader).validate(Path.of("bagdir"));
        assertThat(result.getException()).isNull();
        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.ERROR);
        assertThat(result.getErrorMessages())
                .hasSameElementsAs(List.of(
                        "pos has less than two coordinates: 1.0",
                        "pos has non numeric coordinates: a 5",
                        "lowerCorner is outside RD bounds: -7001 289000", // x too small
                        "upperCorner is outside RD bounds: 300001 629000", // x too large
                        "lowerCorner is outside RD bounds: -7000 288999", // y too small
                        "upperCorner is outside RD bounds: 300000 629001", // y too large
                        "lowerCorner has less than two coordinates: -7000"));
    }

    @Test
    void should_return_SUCCESS_if_value_has_two_coordinates() throws Exception {
        var xml = "<ddm:DDM\n"
                + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "        xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\"\n"
                + "        xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\"\n"
                + "        xmlns:dct=\"http://purl.org/dc/terms/\"\n"
                + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "        xmlns:dcx-gml=\"http://easy.dans.knaw.nl/schemas/dcx/gml/\"\n"
                + "        xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\">\n"
                + "    <ddm:dcmiMetadata>\n"
                + "            <dct:spatial xsi:type='dcx-gml:SimpleGMLType'>"
                + "                <Point xmlns='http://www.opengis.net/gml'>"
                + "                    <pos>1 2</pos>"
                + "                </Point>"
                + "            </dct:spatial>"
                + "    </ddm:dcmiMetadata>"
                + "</ddm:DDM>";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlGmlPointsHaveAtLeastTwoValues(reader).validate(Path.of("bagdir"));
        assertThat(result.getException()).isNull();
        assertThat(result.getStatus()).isEqualTo(RuleResult.Status.SUCCESS);
    }

}
