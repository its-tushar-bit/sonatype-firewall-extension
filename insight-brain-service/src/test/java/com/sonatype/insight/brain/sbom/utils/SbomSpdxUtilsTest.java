/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.ExternalRef;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxPackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class SbomSpdxUtilsTest
{
  @BeforeAll
  public static void initSpdx() {
    SbomSpdxUtils.initSpdxLibrary();
  }

  @Test
  public void testGetRootPackage_json() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-v2_3.json", Format.JSON);
    SpdxPackage rootPackage = SbomSpdxUtils.getRootPackage(spdxDocument);
    assertThat(rootPackage.getName()).contains("sonatype:iq_application_SCM Test 1");
    assertThat(rootPackage.getVersionInfo()).contains("76b10b862e7b42009f2415097620928c");
  }

  @Test
  public void testGetRootPackage_xml() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-v2_3.xml", Format.XML);
    SpdxPackage rootPackage = SbomSpdxUtils.getRootPackage(spdxDocument);
    assertThat(rootPackage.getName()).contains("sonatype:iq_application_SCM Test 1");
    assertThat(rootPackage.getVersionInfo()).contains("76b10b862e7b42009f2415097620928c");
  }

  @Test
  public void testGetRootPackage_noRoot() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-no-rootPackage.json", Format.JSON);
    assertThat(SbomSpdxUtils.getRootPackage(spdxDocument)).isNull();
  }

  @Test
  public void testGetRootPackage_null() throws Exception {
    assertThat(SbomSpdxUtils.getRootPackage(null)).isNull();
  }

  @Test
  public void testGetRootPackage_no_relationship_describe() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-no-relationship-describe.json", Format.JSON);
    SpdxPackage actual = SbomSpdxUtils.getRootPackage(spdxDocument);
    assertThat(actual.getId()).isEqualTo("SPDXRef-Package");
    assertThat(actual.getName().get()).isEqualTo("glibc");
    assertThat(actual.getVersionInfo().get()).isEqualTo("2.11.1");
  }

  @Test
  public void testGetRootPackage_multiple_describes_relationship() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-multiple-describes-relationship.json", Format.JSON);
    SpdxPackage actual = SbomSpdxUtils.getRootPackage(spdxDocument);
    assertThat(actual.getId()).isEqualTo("SPDXRef-997583b27e554729b7e310678f0b5690");
    assertThat(actual.getName().get()).isEqualTo("com.h2database:h2");
    assertThat(actual.getVersionInfo().get()).isEqualTo("2.3.232");
  }

  @Test
  public void testGetRootPackage_single_package() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-single-package.json", Format.JSON);
    SpdxPackage actual = SbomSpdxUtils.getRootPackage(spdxDocument);
    assertThat(actual.getId()).isEqualTo("SPDXRef-997583b27e554729b7e310678f0b5690");
    assertThat(actual.getName().get()).isEqualTo("com.h2database:h2");
    assertThat(actual.getVersionInfo().get()).isEqualTo("2.3.232");
  }

  @Test
  public void testGetAllPackages_json() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-v2_3.json", Format.JSON);
    List<SpdxPackage> allPackages = SbomSpdxUtils.getAllPackages(spdxDocument);
    assertThat(allPackages).hasSize(6);
  }

  @Test
  public void testGetAllPackages_xml() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-v2_3.xml", Format.XML);
    List<SpdxPackage> allPackages = SbomSpdxUtils.getAllPackages(spdxDocument);
    assertThat(allPackages).hasSize(6);
  }

  @Test
  public void testGetAllPackages_noRoot() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-no-rootPackage.json", Format.JSON);
    List<SpdxPackage> allPackages = SbomSpdxUtils.getAllPackages(spdxDocument);
    assertThat(allPackages).hasSize(6);
  }

  @Test
  public void testGetAllPackages_null() throws Exception {
    assertThat(SbomSpdxUtils.getAllPackages(null)).isEmpty();
  }

  @Test
  public void testGetAllVulnerabilities_json() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-v2_3.json", Format.JSON);
    List<ExternalRef> vulnerabilities = SbomSpdxUtils.getAllVulnerabilities(spdxDocument);
    assertThat(vulnerabilities).hasSize(5);
  }

  @Test
  public void testGetAllVulnerabilities_xml() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-v2_3.xml", Format.XML);
    List<ExternalRef> vulnerabilities = SbomSpdxUtils.getAllVulnerabilities(spdxDocument);
    assertThat(vulnerabilities).hasSize(13);
  }

  @Test
  public void testGetAllVulnerabilities_null() throws Exception {
    assertThat(SbomSpdxUtils.getAllVulnerabilities(null)).isEmpty();
  }

  @Test
  public void testGetOrGenerateSpdxSerialNumber() throws InvalidSPDXAnalysisException {
    SpdxDocument doc = new SpdxDocument("docId");
    assertThat(SbomSpdxUtils.getOrGenerateSpdxSerialNumber(doc)).isEqualTo(
        "docId");
  }

  @Test
  public void testGetOrGenerateSpdxSerialNumber_isEmpty() throws InvalidSPDXAnalysisException {
    SpdxDocument doc = new SpdxDocument("");
    assertThat(SbomSpdxUtils.getOrGenerateSpdxSerialNumber(doc)).startsWith(
        "sonatype/spdxdocs/uuid/");
  }

  @Test
  public void testGetOrGenerateSpdxSerialNumber_isNull() {
    SpdxDocument mockDoc = Mockito.mock(SpdxDocument.class);
    when(mockDoc.getDocumentUri()).thenReturn(null);
    assertThat(SbomSpdxUtils.getOrGenerateSpdxSerialNumber(mockDoc)).startsWith(
        "sonatype/spdxdocs/uuid/");
  }

  @Test
  public void testGetSbomCreationDetailsJson_Valid_Json() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-with-metadata.json", Format.JSON);
    String actual = SbomSpdxUtils.getSbomCreationDetailsJson(spdxDocument);
    assertThat(actual).isEqualTo(expectedSbomMetadataJson());
  }

  @Test
  public void testGetSbomCreationDetailsJson_Valid_Xml() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-with-metadata.xml", Format.XML);
    String actual = SbomSpdxUtils.getSbomCreationDetailsJson(spdxDocument);
    assertThat(actual).isEqualTo(expectedSbomMetadataJson());
  }

  @Test
  public void testGetSbomCreationDetailsJson_OnlyTools() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-with-metadata-only-tools.json", Format.JSON);
    String actual = SbomSpdxUtils.getSbomCreationDetailsJson(spdxDocument);
    assertThat(actual).isEqualTo(expectedSbomMetadataJsonOnlyTools());
  }

  @Test
  public void testGetSbomCreationDetailsJson_OnlyCreators() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-with-metadata-only-creators.json", Format.JSON);
    String actual = SbomSpdxUtils.getSbomCreationDetailsJson(spdxDocument);
    assertThat(actual).isEqualTo(expectedSbomMetadataJsonOnlyCreators());
  }

  @Test
  public void testGetSbomCreationDetailsJson_NoDescribes() throws Exception {
    SpdxDocument spdxDocument = getSpdxDocument("spdx-no-relationship-describe.json", Format.JSON);
    String actual = SbomSpdxUtils.getSbomCreationDetailsJson(spdxDocument);
    assertThat(actual).isEqualTo(expectedSbomMetadataJsonNoDescribes());
  }

  @Test
  public void testGetRefIdAndSourceForVulnerability() {
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2024-27088"))
            .isEqualTo(Pair.of("CVE-2024-27088", "NVD"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("http://nvd.nist.gov/vuln/detail/CVE-2016-5007"))
            .isEqualTo(Pair.of("CVE-2016-5007", "NVD"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("http://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2016-5007"))
            .isEqualTo(Pair.of("CVE-2016-5007", "NVD"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("https://osv.dev/vulnerability/BIT-vault-2024-5798"))
            .isEqualTo(Pair.of("BIT-vault-2024-5798", "OSV"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("https://iq.sonatype.dev/ui/links/vln/sonatype-2014-0026"))
            .isEqualTo(Pair.of("sonatype-2014-0026", "SONATYPE"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("https://security.snyk.io/vuln/SNYK-PYTHON-PYMONGO-7172112"))
            .isEqualTo(Pair.of("SNYK-PYTHON-PYMONGO-7172112", "OTHER"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("https://github.com/advisories/GHSA-3jmm-f6jj-rcc3"))
            .isEqualTo(Pair.of("GHSA-3jmm-f6jj-rcc3", "OTHER"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("https://securitylab.github.com/advisories/GHSL-2024-144_JupyterLab/"))
            .isEqualTo(Pair.of("GHSL-2024-144", "OTHER"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("https://errata.almalinux.org/8/ALSA-2024-3128.html"))
            .isEqualTo(Pair.of("ALSA-2024-3128", "OTHER"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("https://securitylab.github.com/advisories/GHSL-2022-097_rudder-server"))
            .isEqualTo(Pair.of("GHSL-2022-097", "OTHER"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("https://securitylab.github.com/advisories#GHSL-2022-097"))
            .isEqualTo(Pair.of("GHSL-2022-097", "OTHER"));
    assertThat(SbomSpdxUtils
        .getRefIdAndSourceForVulnerability("https://access.redhat.com/errata/RHSA-2024:3128"))
            .isEqualTo(Pair.of("RHSA-2024:3128", "OTHER"));

    // invalid
    assertThat(SbomSpdxUtils.getRefIdAndSourceForVulnerability("invalid-url")).isNull();
    assertThat(SbomSpdxUtils.getRefIdAndSourceForVulnerability("https://iq.sonatype.dev/ui/links/vln")).isNull();
    assertThat(SbomSpdxUtils.getRefIdAndSourceForVulnerability(
        "https://osv.dev/vulnerability/BIT-vault-2024-5798/vln")).isNull();
  }

  private static SpdxDocument getSpdxDocument(
      final String fileName,
      Format format) throws IOException, InvalidSPDXAnalysisException, URISyntaxException
  {
    URL resource = SbomSpdxUtilsTest.class.getResource("/SbomSpdxUtilsTest/" + fileName);
    String content = new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8);
    content = content.replace("\"SPDX-2.3\"", "\"SPDX-2.2\"")
        .replace(">SPDX-2.3<", ">SPDX-2.2<");

    return SbomSpdxUtils.parseContentNoValidation(content,
        format == Format.JSON ? SbomFormat.JSON : SbomFormat.XML);
  }

  private String expectedSbomMetadataJson() {
    return "{\"type\":\"APPLICATION\",\"created\":\"2024-03-08T22:14:19Z\",\"creators\":[{\"type\":\"Person\"," +
        "\"name\":\"John Doe\",\"email\":\"john.doe@example.com\"},{\"type\":\"Person\",\"name\":\"Jane Doe\"}," +
        "{\"type\":\"Organization\",\"name\":\"Example Organization\",\"email\":\"example@example.com\"},{\"type\"" +
        ":\"Organization\",\"name\":\"Example Organization\"}],\"tools\":[{\"name\":\"Sonatype IQ Server\"," +
        "\"version\":\"1.175.0-SNAPSHOT\"}]}";
  }

  private String expectedSbomMetadataJsonOnlyTools() {
    return "{\"type\":\"APPLICATION\",\"created\":\"2024-03-08T22:14:19Z\",\"tools\":[{\"name\":" +
        "\"Sonatype IQ Server\",\"version\":\"1.175.0-SNAPSHOT\"}]}";
  }

  private String expectedSbomMetadataJsonOnlyCreators() {
    return "{\"type\":\"APPLICATION\",\"created\":\"2024-03-08T22:14:19Z\",\"creators\":[{\"type\":\"Person\"," +
        "\"name\":\"John Doe\",\"email\":\"john.doe@example.com\"},{\"type\":\"Person\",\"name\":\"Jane Doe\"}," +
        "{\"type\":\"Person\",\"name\":\"Joe Smith\"},{\"type\":\"Organization\",\"name\":\"Example Organization\"," +
        "\"email\":\"example@example.com\"},{\"type\":\"Organization\",\"name\":\"Example Organization2\"}]}";
  }

  private String expectedSbomMetadataJsonNoDescribes() {
    return "{\"type\":\"SOURCE\",\"created\":\"2010-01-29T18:30:22Z\",\"creators\":[{\"type\":\"Organization\"," +
        "\"name\":\"ExampleCodeInspect\"},{\"type\":\"Person\",\"name\":\"Jane Doe\"}]," +
        "\"tools\":[{\"name\":\"LicenseFind\",\"version\":\"1.0\"}]}";
  }
}
