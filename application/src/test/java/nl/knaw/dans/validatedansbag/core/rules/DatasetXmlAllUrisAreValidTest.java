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

public class DatasetXmlAllUrisAreValidTest extends RuleTestFixture {
    @Test
    void should_return_SUCCESS_if_all_uris_valid() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<ddm:DDM xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\" xmlns=\"http://easy.dans.knaw.nl/schemas/bag/metadata/files/\" xmlns:abr=\"http://www.den.nl/standaard/166/Archeologisch-Basisregister/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\" xmlns:dcx-gml=\"http://easy.dans.knaw.nl/schemas/dcx/gml/\" xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >\n"
                + "    <ddm:profile>\n"
                + "        <dc:title>PAN-00008136 - knobbed sickle</dc:title>\n"
                + "        <dcterms:description xml:lang=\"en\">This find is registered at Portable Antiquities of the Netherlands with number PAN-00008136</dcterms:description>\n"
                + "        <dcx-dai:creatorDetails>\n"
                + "            <dcx-dai:organization>\n"
                + "                <dcx-dai:name xml:lang=\"en\">Portable Antiquities of the Netherlands</dcx-dai:name>\n"
                + "                <dcx-dai:role>DataCurator</dcx-dai:role>\n"
                + "            </dcx-dai:organization>\n"
                + "        </dcx-dai:creatorDetails>\n"
                + "        <ddm:created>2017-10-23T17:06:11+02:00</ddm:created>\n"
                + "        <ddm:available>2017-10-23T17:06:11+02:00</ddm:available>\n"
                + "        <ddm:audience>D37000</ddm:audience>\n"
                + "        <ddm:accessRights>OPEN_ACCESS</ddm:accessRights>\n"
                + "    </ddm:profile>\n"
                + "    <ddm:dcmiMetadata>\n"
                + "        <dcterms:spatial>Overbetuwe</dcterms:spatial>\n"
                + "        <dcterms:isFormatOf>PAN-00008136</dcterms:isFormatOf>\n"
                + "        <ddm:references href=\"https://www.portable-antiquities.nl/pan/#/object/public/8136\">Portable Antiquities of The Netherlands</ddm:references>\n"
                + "        <ddm:references scheme=\"URL\">http://abc.def</ddm:references>\n"
                + "        <ddm:references scheme=\"DOI\">10.17026/test-123-456</ddm:references>\n"
                + "        <ddm:references scheme=\"DOI\">https://dx.doi.org/doi:10.17026/test-123-456</ddm:references>\n"
                + "        <ddm:references scheme=\"DOI\" href=\"https://dx.doi.org/doi:10.17026/test-123-456\">a doi referencing my dataset</ddm:references>\n"
                + "        <ddm:references scheme=\"DOI\">http://doi.org/10.17026/test-123-456</ddm:references>\n"
                + "        <ddm:references scheme=\"URN\">urn:uuid:6e8bc430-9c3a-11d9-9669-0800200c9a66()+,-\\.:=@;$_!*'%/?#</ddm:references>\n"
                + "        <ddm:references scheme=\"id-type:URL\">http://abc.def</ddm:references>\n"
                + "        <ddm:references scheme=\"id-type:DOI\">10.17026/test-123-456</ddm:references>\n"
                + "        <ddm:references scheme=\"id-type:DOI\">https://dx.doi.org/doi:10.17026/test-123-456</ddm:references>\n"
                + "        <ddm:references scheme=\"id-type:DOI\" href=\"https://dx.doi.org/doi:10.17026/test-123-456\">a doi referencing my dataset</ddm:references>\n"
                + "        <ddm:references scheme=\"id-type:DOI\">http://doi.org/10.17026/test-123-456</ddm:references>\n"
                + "        <ddm:references scheme=\"id-type:URN\">urn:uuid:6e8bc430-9c3a-11d9-9669-0800200c9a66()+,-\\.:=@;$_!*'%/?#</ddm:references>\n"
                + "        <ddm:subject schemeURI=\"https://data.cultureelerfgoed.nl/term/id/pan/PAN\" subjectScheme=\"PAN thesaurus ideaaltypes\" valueURI=\"https://data.cultureelerfgoed.nl/term/id/pan/17-01-01\" xml:lang=\"en\">knobbed sickle</ddm:subject>\n"
                + "        <ddm:subject schemeURI=\"http://vocab.getty.edu/aat/\" subjectScheme=\"Art and Architecture Thesaurus\" valueURI=\"http://vocab.getty.edu/aat/300264860\" xml:lang=\"en\">Unknown</ddm:subject>\n"
                + "        <dc:subject>metaal</dc:subject>\n"
                + "        <dc:subject>koperlegering</dc:subject>\n"
                + "        <dcterms:identifier>PAN-00008136</dcterms:identifier>\n"
                + "        <dcterms:temporal xsi:type=\"abr:ABRperiode\">BRONSMB</dcterms:temporal>\n"
                + "        <dcterms:temporal xsi:type=\"abr:ABRperiode\">BRONSL</dcterms:temporal>\n"
                + "        <dcterms:temporal>-1500 until -800</dcterms:temporal>\n"
                + "        <dc:language xsi:type=\"dcterms:ISO639-2\">eng</dc:language>\n"
                + "        <dc:publisher xmlns:dc=\"http://purl.org/dc/terms/\">DANS/KNAW</dc:publisher>\n"
                + "        <dc:type xsi:type=\"dcterms:DCMIType\" xmlns:dc=\"http://purl.org/dc/terms/\">Dataset</dc:type>\n"
                + "        <dc:format xsi:type=\"dcterms:IMT\">image/jpeg</dc:format>\n"
                + "        <dc:format xsi:type=\"dcterms:IMT\">application/xml</dc:format>\n"
                + "        <dcterms:license xsi:type=\"dcterms:URI\">http://creativecommons.org/licenses/by-nc-sa/4.0/</dcterms:license>\n"
                + "        <dcterms:rightsHolder>Vrije Universiteit Amsterdam</dcterms:rightsHolder>\n"
                + "        <dcterms:identifier xsi:type=\"id-type:DOI\">10.1234/fantasy-doi-id</dcterms:identifier>\n"
                + "        <dcterms:identifier xsi:type=\"id-type:DOI\">10.1234.567/issn-987-654</dcterms:identifier>\n"
                + "    </ddm:dcmiMetadata>\n"
                + "</ddm:DDM>\n";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlAllUrlsAreValid(xmlReader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void should_return_ERROR_if_some_uris_invalid() throws Exception {
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<ddm:DDM xmlns:ddm=\"http://schemas.dans.knaw.nl/dataset/ddm-v2/\" xmlns=\"http://easy.dans.knaw.nl/schemas/bag/metadata/files/\" xmlns:abr=\"http://www.den.nl/standaard/166/Archeologisch-Basisregister/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:dcx-dai=\"http://easy.dans.knaw.nl/schemas/dcx/dai/\" xmlns:dcx-gml=\"http://easy.dans.knaw.nl/schemas/dcx/gml/\" xmlns:id-type=\"http://easy.dans.knaw.nl/schemas/vocab/identifier-type/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                + "    <ddm:profile>\n"
                + "        <dc:title>PAN-00008136 - knobbed sickle</dc:title>\n"
                + "        <dcterms:description xml:lang=\"en\">This find is registered at Portable Antiquities of the Netherlands with number PAN-00008136</dcterms:description>\n"
                + "        <dcx-dai:creatorDetails>\n"
                + "            <dcx-dai:organization>\n"
                + "                <dcx-dai:name xml:lang=\"en\">Portable Antiquities of the Netherlands</dcx-dai:name>\n"
                + "                <dcx-dai:role>DataCurator</dcx-dai:role>\n"
                + "            </dcx-dai:organization>\n"
                + "        </dcx-dai:creatorDetails>\n"
                + "        <ddm:created>2017-10-23T17:06:11+02:00</ddm:created>\n"
                + "        <ddm:available>2017-10-23T17:06:11+02:00</ddm:available>\n"
                + "        <ddm:audience>D37000</ddm:audience>\n"
                + "        <ddm:accessRights>OPEN_ACCESS</ddm:accessRights>\n"
                + "    </ddm:profile>\n"
                + "    <ddm:dcmiMetadata>\n"
                + "        <dcterms:spatial>Overbetuwe</dcterms:spatial>\n"
                + "        <dcterms:isFormatOf>PAN-00008136</dcterms:isFormatOf>\n"
                // INVALID
                + "        <ddm:relation href=\"dx.doi.org/10.17026/dans-xrt-q9cp\">Thematische collectie: COOL5-18, Pre-COOL en COOLspeciaal</ddm:relation>"
                // INVALID
                + "        <ddm:references href=\"xttps://www.portable-antiquities.nl/pan/#/object/public/8136\">Portable Antiquities of The Netherlands</ddm:references>\n"
                // INVALID
                + "        <ddm:references scheme=\"URL\">xttp://abc.def</ddm:references>\n"
                + "        <ddm:references scheme=\"DOI\">99.1234.abc</ddm:references>\n"
                + "        <ddm:references scheme=\"URN\">uuid:6e8bc430-9c3a-11d9-9669-0800200c9a66</ddm:references>\n"
                + "        <ddm:isFormatOf scheme=\"id-type:DOI\">joopajoo</ddm:isFormatOf>\n"
                + "        <ddm:isFormatOf scheme=\"id-type:URN\">niinpä</ddm:isFormatOf>\n"
                // INVALID 2x
                + "        <ddm:subject schemeURI=\"xttps://data.cultureelerfgoed.nl/term/id/pan/PAN\" subjectScheme=\"PAN thesaurus ideaaltypes\" valueURI=\"xttps://data.cultureelerfgoed.nl/term/id/pan/17-01-01\" xml:lang=\"en\">knobbed sickle</ddm:subject>\n"
                + "        <ddm:subject schemeURI=\"http://vocab.getty.edu/aat/\" subjectScheme=\"Art and Architecture Thesaurus\" valueURI=\"http://vocab.getty.edu/aat/300264860\" xml:lang=\"en\">Unknown</ddm:subject>\n"
                + "        <dc:subject>metaal</dc:subject>\n"
                + "        <dc:subject>koperlegering</dc:subject>\n"
                + "        <dcterms:identifier>PAN-00008136</dcterms:identifier>\n"
                + "        <dcterms:temporal xsi:type=\"abr:ABRperiode\">BRONSMB</dcterms:temporal>\n"
                + "        <dcterms:temporal xsi:type=\"abr:ABRperiode\">BRONSL</dcterms:temporal>\n"
                + "        <dcterms:temporal>-1500 until -800</dcterms:temporal>\n"
                + "        <dc:language xsi:type=\"dcterms:ISO639-2\">eng</dc:language>\n"
                + "        <dc:publisher xmlns:dc=\"http://purl.org/dc/terms/\">DANS/KNAW</dc:publisher>\n"
                + "        <dc:type xsi:type=\"dcterms:DCMIType\" xmlns:dc=\"http://purl.org/dc/terms/\">Dataset</dc:type>\n"
                + "        <dc:format xsi:type=\"dcterms:IMT\">image/jpeg</dc:format>\n"
                + "        <dc:format xsi:type=\"dcterms:IMT\">application/xml</dc:format>\n"
                // INVALID
                + "        <dcterms:license xsi:type=\"dcterms:URI\">ettp://creativecommons.org/licenses/by-nc-sa/4.0/</dcterms:license>\n"
                + "        <dcterms:rightsHolder>Vrije Universiteit Amsterdam</dcterms:rightsHolder>\n"
                + "        <dcterms:identifier xsi:type=\"id-type:DOI\">10.1234/fantasy-doi-id</dcterms:identifier>\n"
                + "        <dcterms:identifier xsi:type=\"id-type:DOI\">10.1234.567/issn-987-654</dcterms:identifier>\n"
                + "    </ddm:dcmiMetadata>\n"
                + "</ddm:DDM>\n";

        var document = parseXmlString(xml);
        var reader = Mockito.spy(new XmlReaderImpl());

        Mockito.doReturn(document).when(reader).readXmlFile(Mockito.any());

        var result = new DatasetXmlAllUrlsAreValid(reader).validate(Path.of("bagdir"));
        assertEquals(RuleResult.Status.ERROR, result.getStatus());
        assertEquals(6, result.getErrorMessages().size());
    }

}
