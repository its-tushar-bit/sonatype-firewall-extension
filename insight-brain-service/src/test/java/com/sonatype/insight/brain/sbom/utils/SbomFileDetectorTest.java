/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomFileDetectorTest
{
  private SbomFileDetector detector;

  @Before
  public void before() {
    detector = new SbomFileDetector();
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_Json_Valid_1_4() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/json", null, "1.4", "CycloneDx", "json", 1, 1,
            "example-sbom-application-1.4",
            "0.0.1");
    getSbomMetadata("cyclonedx-valid-v1_4-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_Json_Valid_1_5() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/json", null, "1.5", "CycloneDx", "json", 1, 1,
            "example-sbom-application-1.5",
            "1.0.1");
    getSbomMetadata("cyclonedx-valid-v1_5-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_0() {
    SbomDetectionResult expected =
        createExpectedResult(false, "application/xml", "CycloneDX XML 1.0 version is not supported");
    getSbomMetadata("cyclonedx-valid-v1_0.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_1() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/xml", null, "1.1", "CycloneDx", "xml", 1, 0, null, null);
    getSbomMetadata("cyclonedx-valid-v1_1.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_2() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/xml", null, "1.2", "CycloneDx", "xml", 2, 0, "Acme Application",
            "9.1.1");
    getSbomMetadata("cyclonedx-valid-v1_2.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_3() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/xml", null, "1.3", "CycloneDx", "xml", 2, 0, "Acme Application",
            "9.1.1");
    getSbomMetadata("cyclonedx-valid-v1_3.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDxVulnerabilityExtension_XML_Valid_1_4() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/xml", null, "1.4", "CycloneDx", "xml", 1, 0, null, null);
    getSbomMetadata("cyclonedx-vulnerability-ext-v1_4.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_5() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/xml", null, "1.5", "CycloneDx", "xml", 1, 1, null, null);
    getSbomMetadata("cyclonedx-valid-v1_5-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_Valid_Xml5() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/xml", null, "1.5", "CycloneDx", "xml", 1, 0, null, null);
    getSbomMetadata("cyclonedx-valid-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_OtherExtension() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/xml", null, "1.4", "CycloneDx", "xml", 2, 0, "Acme Application",
            "9.1.1");
    getSbomMetadata("cyclonedx-valid-bom-uknown-extension.abc", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_XML_Valid_2_3() {
    SbomDetectionResult expected =
        createExpectedResult(true, "application/xml", null, "2.3", "SPDX", "xml", 6, 13,
            "sonatype:iq_application_SCM Test 1",
            "76b10b862e7b42009f2415097620928c");
    getSbomMetadata("spdx-v2_3-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_XML_Valid_2_2() {
    SbomDetectionResult expected =
        createExpectedResult(false, "application/xml", "SPDX 2.2 version is not supported");
    getSbomMetadata("spdx-v2_2-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_Json_Valid_2_3() {
    SbomDetectionResult expected2 =
        createExpectedResult(true, "application/json", null, "2.3", "SPDX", "json", 6, 5,
            "sonatype:iq_application_SCM Test 1", "76b10b862e7b42009f2415097620928c");
    getSbomMetadata("spdx-v2_3-json.tmp", expected2);
  }

  @Test
  public void testGetSbomMetadata_Other_Xml() {
    SbomDetectionResult expected =
        createExpectedResult(false, "application/xml", "Not a valid/supported sbom file");
    getSbomMetadata("non-sbom-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_Other_Json() {
    SbomDetectionResult expected =
        createExpectedResult(false, "application/json", "Not a valid/supported sbom file");
    getSbomMetadata("non-sbom-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_InvalidJson() {
    SbomDetectionResult expected =
        createExpectedResult(false, "text/plain", "provided file type is not a supported SBOM file type");
    getSbomMetadata("scyclonedx-invalid-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_InvalidXml() {
    SbomDetectionResult expected =
        createExpectedResult(false, "application/xml", "not a valid CycloneDx SBOM file");
    getSbomMetadata("cyclonedx-invalid-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_InvalidJson() {
    SbomDetectionResult expected =
        createExpectedResult(false, "application/json", "not a valid SPDX SBOM file");
    getSbomMetadata("spdx-invalid-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_InvalidXml() {
    SbomDetectionResult expected =
        createExpectedResult(false, "application/xml", "Not a valid/supported sbom file");
    getSbomMetadata("spdx-invalid-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_Other_Binary() {
    SbomDetectionResult expected =
        createExpectedResult(false, "application/java-vm", "provided file type is not a supported SBOM file type");
    getSbomMetadata("test.bin", expected);
  }

  @Test
  public void testGetSbomMetadata_Other_Text() {
    SbomDetectionResult expected =
        createExpectedResult(false, "text/plain", "provided file type is not a supported SBOM file type");
    getSbomMetadata("test.tt", expected);
  }

  @Test
  public void testGetSbomMetadata_Other_Text_UnsafeContent_CycloneDx() throws IOException {
    File fileToDetect = getTestFile("unsafe-plain-text-cdx.tt");
    String sbomContent = FileUtils.readFileToString(fileToDetect, StandardCharsets.UTF_8);
    assertThat(detector.isPlainTextValidXml(sbomContent)).isFalse();
  }

  @Test
  public void testGetSbomMetadata_Other_Text_UnsafeContent_SPDX() throws IOException {
    File fileToDetect = getTestFile("unsafe-plain-text-spdx.tt");
    String sbomContent = FileUtils.readFileToString(fileToDetect, StandardCharsets.UTF_8);
    assertThat(detector.isPlainTextValidXml(sbomContent)).isFalse();
  }

  @Test
  public void testGetSbomMetadata_Other_Text_SafeContent() throws IOException {
    File fileToDetect = getTestFile("safe-plain-text.tt");
    String sbomContent = FileUtils.readFileToString(fileToDetect, StandardCharsets.UTF_8);
    assertThat(detector.isPlainTextValidXml(sbomContent)).isTrue();
  }

  private void getSbomMetadata(String fileName, SbomDetectionResult expected) {
    File fileToDetect = getTestFile(fileName);
    SbomDetectionResult resultFromFile = detector.getSbomDetectionResult(fileToDetect);
    InputStream inputStream = getInputStreamFromFile(fileName);
    SbomDetectionResult resultFromString = detector.getSbomDetectionResult(inputStream);

    verifySbomDetectionResult(resultFromFile, expected);
    verifySbomDetectionResult(resultFromString, expected);
  }

  private static SbomDetectionResult createExpectedResult(boolean isSbom, String mimeType, String errorMessage) {
    return createExpectedResult(isSbom, mimeType, errorMessage, null, null, null, 0, 0, null, null);
  }

  private static SbomDetectionResult createExpectedResult(
      boolean isSbom,
      String mimeType,
      String errorMessage,
      String version,
      String specification,
      String format,
      int componentCount,
      int vulnerabilityCount,
      String applicationName,
      String applicationVersion)
  {
    SbomDetectionResult expected = new SbomDetectionResult();
    expected.isSbom = isSbom;
    expected.mimeType = mimeType;
    if (errorMessage != null) {
      expected.errorMessage = errorMessage;
    }
    else {
      expected.summary = new SbomSummary();
      expected.summary.version = version;
      expected.summary.specification = specification;
      expected.summary.format = format;
      expected.summary.componentCount = componentCount;
      expected.summary.vulnerabilityCount = vulnerabilityCount;
      expected.summary.applicationName = applicationName;
      expected.summary.applicationVersion = applicationVersion;
    }
    return expected;
  }

  private void verifySbomDetectionResult(
      SbomDetectionResult result,
      SbomDetectionResult expected)
  {
    assertThat(result.isSbom).isEqualTo(expected.isSbom);
    assertThat(result.mimeType).isEqualTo(expected.mimeType);
    assertThat(result.errorMessage).isEqualTo(expected.errorMessage);
    if (expected.summary != null) {
      assertThat(result.summary.version).isEqualTo(expected.summary.version);
      assertThat(result.summary.specification).isEqualTo(expected.summary.specification);
      assertThat(result.summary.format).isEqualTo(expected.summary.format);
      assertThat(result.summary.componentCount).isEqualTo(expected.summary.componentCount);
      assertThat(result.summary.vulnerabilityCount).isEqualTo(expected.summary.vulnerabilityCount);
      assertThat(result.summary.applicationName).isEqualTo(expected.summary.applicationName);
      assertThat(result.summary.applicationVersion).isEqualTo(expected.summary.applicationVersion);
    }
  }

  private File getTestFile(final String fileName) {
    URL resource = SbomFileDetectorTest.class.getResource("/SbomFileDetectorTest/" + fileName);
    return new File(Objects.requireNonNull(resource).getFile());
  }

  private InputStream getInputStreamFromFile(final String fileName) {
    return SbomFileDetectorTest.class.getResourceAsStream("/SbomFileDetectorTest/" + fileName);
  }
}
