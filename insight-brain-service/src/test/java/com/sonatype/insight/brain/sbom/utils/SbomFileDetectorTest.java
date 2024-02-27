/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.File;
import java.net.URL;
import java.util.Objects;

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
    File toDetect = getTestFile("cyclonedx-valid-v1_4.json");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/json");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("1.4");
    assertThat(result.summary.specification).isEqualTo("CycloneDx");
    assertThat(result.summary.format).isEqualTo("json");
    assertThat(result.summary.componentCount).isEqualTo(1);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(1);
    assertThat(result.summary.applicationName).isEqualTo("example-sbom-application-1.4");
    assertThat(result.summary.applicationVersion).isEqualTo("0.0.1");
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_Json_Valid_1_5() {
    File toDetect = getTestFile("cyclonedx-valid-v1_5.json");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/json");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("1.5");
    assertThat(result.summary.specification).isEqualTo("CycloneDx");
    assertThat(result.summary.format).isEqualTo("json");
    assertThat(result.summary.componentCount).isEqualTo(1);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(1);
    assertThat(result.summary.applicationName).isEqualTo("example-sbom-application-1.5");
    assertThat(result.summary.applicationVersion).isEqualTo("1.0.1");
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_0() {
    File toDetect = getTestFile("cyclonedx-valid-v1_0.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isFalse();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isEqualTo("CycloneDX XML 1.0 version is not supported");
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_1() {
    File toDetect = getTestFile("cyclonedx-valid-v1_1.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("1.1");
    assertThat(result.summary.specification).isEqualTo("CycloneDx");
    assertThat(result.summary.format).isEqualTo("xml");
    assertThat(result.summary.componentCount).isEqualTo(1);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(0);
    assertThat(result.summary.applicationName).isNull();
    assertThat(result.summary.applicationVersion).isNull();
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_2() {
    File toDetect = getTestFile("cyclonedx-valid-v1_2.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("1.2");
    assertThat(result.summary.specification).isEqualTo("CycloneDx");
    assertThat(result.summary.format).isEqualTo("xml");
    assertThat(result.summary.componentCount).isEqualTo(2);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(0);
    assertThat(result.summary.applicationName).isEqualTo("Acme Application");
    assertThat(result.summary.applicationVersion).isEqualTo("9.1.1");
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_3() {
    File toDetect = getTestFile("cyclonedx-valid-v1_3.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("1.3");
    assertThat(result.summary.specification).isEqualTo("CycloneDx");
    assertThat(result.summary.format).isEqualTo("xml");
    assertThat(result.summary.componentCount).isEqualTo(2);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(0);
    assertThat(result.summary.applicationName).isEqualTo("Acme Application");
    assertThat(result.summary.applicationVersion).isEqualTo("9.1.1");
  }

  @Test
  public void testGetSbomMetadata_CycloneDxVulnerabilityExtension_XML_Valid_1_4() {
    File toDetect = getTestFile("cyclonedx-vulnerability-ext-v1_4.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("1.4");
    assertThat(result.summary.specification).isEqualTo("CycloneDx");
    assertThat(result.summary.format).isEqualTo("xml");
    assertThat(result.summary.componentCount).isEqualTo(1);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(0);
    assertThat(result.summary.applicationName).isNull();
    assertThat(result.summary.applicationVersion).isNull();
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_5() {
    File toDetect = getTestFile("cyclonedx-valid-v1_5.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("1.5");
    assertThat(result.summary.specification).isEqualTo("CycloneDx");
    assertThat(result.summary.format).isEqualTo("xml");
    assertThat(result.summary.componentCount).isEqualTo(1);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(1);
    assertThat(result.summary.applicationName).isNull();
    assertThat(result.summary.applicationVersion).isNull();
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_OtherExtension() {
    File toDetect = getTestFile("cyclonedx-valid-bom-uknown-extension.abc");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("1.4");
    assertThat(result.summary.specification).isEqualTo("CycloneDx");
    assertThat(result.summary.format).isEqualTo("xml");
    assertThat(result.summary.componentCount).isEqualTo(2);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(0);
    assertThat(result.summary.applicationName).isEqualTo("Acme Application");
    assertThat(result.summary.applicationVersion).isEqualTo("9.1.1");
  }

  @Test
  public void testGetSbomMetadata_SPDX_XML_Valid_2_3() {
    File toDetect = getTestFile("spdx-v2_3.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("2.3");
    assertThat(result.summary.specification).isEqualTo("SPDX");
    assertThat(result.summary.format).isEqualTo("xml");
    assertThat(result.summary.componentCount).isEqualTo(6);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(13);
    assertThat(result.summary.applicationName).isEqualTo("sonatype:iq_application_SCM Test 1");
    assertThat(result.summary.applicationVersion).isEqualTo("76b10b862e7b42009f2415097620928c");
  }

  @Test
  public void testGetSbomMetadata_SPDX_XML_Valid_2_2() {
    File toDetect = getTestFile("spdx-v2_2.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isFalse();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isEqualTo("SPDX 2.2 version is not supported");
  }

  @Test
  public void testGetSbomMetadata_SPDX_Json_Valid_2_3() {
    File toDetect = getTestFile("spdx-v2_3.json");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isTrue();
    assertThat(result.mimeType).isEqualTo("application/json");
    assertThat(result.errorMessage).isNull();
    assertThat(result.summary.version).isEqualTo("2.3");
    assertThat(result.summary.specification).isEqualTo("SPDX");
    assertThat(result.summary.format).isEqualTo("json");
    assertThat(result.summary.componentCount).isEqualTo(6);
    assertThat(result.summary.vulnerabilityCount).isEqualTo(5);
    assertThat(result.summary.applicationName).isEqualTo("sonatype:iq_application_SCM Test 1");
    assertThat(result.summary.applicationVersion).isEqualTo("76b10b862e7b42009f2415097620928c");
  }

  @Test
  public void testGetSbomMetadata_Other_Xml() {
    File toDetect = getTestFile("non-sbom.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isFalse();
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.errorMessage).isEqualTo("Not a valid/supported sbom file");
  }

  @Test
  public void testGetSbomMetadata_Other_Json() {
    File toDetect = getTestFile("non-sbom.json");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.isSbom).isFalse();
    assertThat(result.mimeType).isEqualTo("application/json");
    assertThat(result.errorMessage).isEqualTo("Not a valid/supported sbom file");
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_InvalidJson() {
    File toDetect = getTestFile("cyclonedx-invalid.json");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.mimeType).isEqualTo("application/json");
    assertThat(result.isSbom).isFalse();
    assertThat(result.errorMessage).isEqualTo("not a valid CycloneDx SBOM file");
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_InvalidXml() {
    File toDetect = getTestFile("cyclonedx-invalid.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.isSbom).isFalse();
    assertThat(result.errorMessage).isEqualTo("not a valid CycloneDx SBOM file");
  }

  @Test
  public void testGetSbomMetadata_SPDX_InvalidJson() {
    File toDetect = getTestFile("spdx-invalid.json");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.mimeType).isEqualTo("application/json");
    assertThat(result.isSbom).isFalse();
    assertThat(result.errorMessage).isEqualTo("not a valid SPDX SBOM file");
  }

  @Test
  public void testGetSbomMetadata_SPDX_InvalidXml() {
    File toDetect = getTestFile("spdx-invalid.xml");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.mimeType).isEqualTo("application/xml");
    assertThat(result.isSbom).isFalse();
    assertThat(result.errorMessage).isEqualTo("Not a valid/supported sbom file");
  }

  @Test
  public void testGetSbomMetadata_Other_Binary() {
    File toDetect = getTestFile("test.bin");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.mimeType).isEqualTo("application/java-vm");
    assertThat(result.isSbom).isFalse();
    assertThat(result.errorMessage).isEqualTo("provided file type is not a supported SBOM file type");
  }

  @Test
  public void testGetSbomMetadata_Other_Text() {
    File toDetect = getTestFile("test.tt");
    SbomDetectionResult result = detector.getSbomMetadata(toDetect);
    assertThat(result.mimeType).isEqualTo("text/plain");
    assertThat(result.isSbom).isFalse();
    assertThat(result.errorMessage).isEqualTo("provided file type is not a supported SBOM file type");
  }

  private File getTestFile(final String fileName) {
    URL resource = SbomFileDetectorTest.class.getResource("/SbomFileDetectorTest/" + fileName);
    return new File(Objects.requireNonNull(resource).getFile());
  }
}
