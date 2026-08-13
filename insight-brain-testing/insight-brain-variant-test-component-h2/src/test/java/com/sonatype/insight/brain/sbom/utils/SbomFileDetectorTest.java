/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class SbomFileDetectorTest
    extends AbstractComponentH2Test
{
  @Inject
  private SbomFileDetector detector;

  @BeforeEach
  public void before() {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(false);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_Json_Valid_1_4() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/json", "1.4", "CycloneDx", "json", 1, 1,
        "example-sbom-application-1.4", "0.0.1");

    checkSbomMetadata("cyclonedx-valid-v1_4-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_Json_Valid_1_5() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/json", "1.5", "CycloneDx", "json", 1, 1,
        "example-sbom-application-1.5", "1.0.1");

    checkSbomMetadata("cyclonedx-valid-v1_5-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_0() throws Exception {
    SbomDetectionResult expected =
        createInvalidSbomExpectedResult("application/xml", "CycloneDX XML 1.0 version is not supported", null, false);
    checkSbomMetadata("cyclonedx-valid-v1_0.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_1() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/xml", "1.1", "CycloneDx", "xml", 1, 0,
        null, null);

    checkSbomMetadata("cyclonedx-valid-v1_1.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_2() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/xml", "1.2", "CycloneDx", "xml", 2, 1,
        "Acme Application", "9.1.1");

    checkSbomMetadata("cyclonedx-valid-v1_2.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_3() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/xml", "1.3", "CycloneDx", "xml", 2, 1,
        "Acme Application", "9.1.1");
    checkSbomMetadata("cyclonedx-valid-v1_3.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDxVulnerabilityExtension_XML_Valid_1_4() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/xml", "1.4", "CycloneDx", "xml", 1, 1,
        null, null);
    checkSbomMetadata("cyclonedx-vulnerability-ext-v1_4.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_1_5() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/xml", "1.5", "CycloneDx", "xml", 1, 1,
        null, null);
    checkSbomMetadata("cyclonedx-valid-v1_5-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Invalid_1_4() throws Exception {
    List<String> expectedErrors = List.of(
        "Line: 22, Column: 16, Path: //bom[1]/components[1]/component[1], Error: cvc-complex-type.2.4.a: Invalid content was found starting with element '{\"http://cyclonedx.org/schema/bom/1.4\":version}'. One of '{\"http://cyclonedx.org/schema/bom/1.4\":name}' is expected.");
    SbomDetectionResult expected = createExpectedResult(true, false, true, "application/xml",
        "Not a valid CycloneDX SBOM file.", expectedErrors, "1.4", "CycloneDx", "xml", 1, 1,
        "insight-scanner", "2.36.19-SNAPSHOT");
    checkSbomMetadata("cyclonedx-invalid-v1_4-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Invalid_1_4_ignoreValidationError() throws Exception {
    SbomDetectionResult expected = createValidationIgnoredExpectedResult("application/xml", "1.4", "CycloneDx", "xml",
        1, 1, "insight-scanner", "2.36.19-SNAPSHOT");
    checkSbomMetadata("cyclonedx-invalid-v1_4-xml.tmp", expected, true);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Invalid_1_4_SKIP_SBOM_IMPORT_VALIDATION() throws Exception {
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);
    SbomDetectionResult expected = createValidationIgnoredExpectedResult("application/xml", "1.4", "CycloneDx", "xml",
        1, 1, "insight-scanner", "2.36.19-SNAPSHOT");
    checkSbomMetadata("cyclonedx-invalid-v1_4-xml.tmp", expected, false);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_Valid_Xml5() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/xml", "1.5", "CycloneDx", "xml", 1, 0,
        null, null);
    checkSbomMetadata("cyclonedx-valid-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_XML_Valid_OtherExtension() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/xml", "1.4", "CycloneDx", "xml", 2, 0,
        "Acme Application", "9.1.1");
    checkSbomMetadata("cyclonedx-valid-bom-uknown-extension.abc", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_XML_Valid_2_3() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/xml", "2.3", "SPDX", "xml", 6, 13,
        "sonatype:iq_application_SCM Test 1", "76b10b862e7b42009f2415097620928c");
    checkSbomMetadata("spdx-v2_3-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_XML_Valid_2_2() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/xml", "2.2", "SPDX", "xml", 6, 13,
        "sonatype:iq_application_SCM Test 1", "76b10b862e7b42009f2415097620928c");

    checkSbomMetadata("spdx-v2_2-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_Json_Valid_2_3() throws Exception {
    SbomDetectionResult expected = createValidSbomExpectedResult("application/json", "2.3", "SPDX", "json", 6, 5,
        "sonatype:iq_application_SCM Test 1", "76b10b862e7b42009f2415097620928c");
    checkSbomMetadata("spdx-v2_3-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_Other_Xml() throws Exception {
    SbomDetectionResult expected = createBinaryExpectedResult("application/xml", "Not a valid/supported SBOM file.");
    checkSbomMetadata("non-sbom-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_Other_Json() throws Exception {
    SbomDetectionResult expected = createBinaryExpectedResult("application/json", "Not a valid/supported SBOM file.");
    checkSbomMetadata("non-sbom-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_InvalidJson() throws Exception {
    SbomDetectionResult expected =
        createBinaryExpectedResult("text/plain", "Provided file type is not a supported SBOM file type.");
    checkSbomMetadata("scyclonedx-invalid-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_InvalidXml() throws Exception {
    List<String> expectedErrors = List.of(
        "Line: 24, Column: 14, Error: The end-tag for element type \"component\" must end with a '>' delimiter.");
    SbomDetectionResult expected =
        createExpectedResult(true, false, false, "application/xml", "Not a valid CycloneDX SBOM file.",
            expectedErrors, null, "CycloneDx", "xml", 0, 0, null, null);
    checkSbomMetadata("cyclonedx-invalid-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_CycloneDx_InvalidXml2() throws Exception {
    List<String> expectedErrors = List.of(
        "Line: 3612, Column: 59, Path: //bom[1]/components[1]/component[113]/externalReferences[1]/reference[1]/url[1], Error: cvc-datatype-valid.1.2.1: 'git@github.com:colorjs/color-name.git' is not a valid value for 'anyURI'.",
        "Line: 3612, Column: 59, Path: //bom[1]/components[1]/component[113]/externalReferences[1]/reference[1]/url[1], Error: cvc-type.3.1.3: The value 'git@github.com:colorjs/color-name.git' of element 'url' is not valid.",
        "Line: 5946, Column: 74, Path: //bom[1]/components[1]/component[190]/externalReferences[1]/reference[1]/url[1], Error: cvc-datatype-valid.1.2.1: 'git@github.com:follow-redirects/follow-redirects.git' is not a valid value for 'anyURI'.",
        "Line: 5946, Column: 74, Path: //bom[1]/components[1]/component[190]/externalReferences[1]/reference[1]/url[1], Error: cvc-type.3.1.3: The value 'git@github.com:follow-redirects/follow-redirects.git' of element 'url' is not valid.",
        "Line: 8857, Column: 67, Path: //bom[1]/components[1]/component[279]/externalReferences[1]/reference[1]/url[1], Error: cvc-datatype-valid.1.2.1: 'git@github.com:jprichardson/node-jsonfile.git' is not a valid value for 'anyURI'.",
        "Line: 8857, Column: 67, Path: //bom[1]/components[1]/component[279]/externalReferences[1]/reference[1]/url[1], Error: cvc-type.3.1.3: The value 'git@github.com:jprichardson/node-jsonfile.git' of element 'url' is not valid.",
        "Line: 14669, Column: 56, Path: //bom[1]/components[1]/component[463]/externalReferences[1]/reference[1]/url[1], Error: cvc-datatype-valid.1.2.1: 'git@github.com:lupomontero/psl.git' is not a valid value for 'anyURI'.",
        "Line: 14669, Column: 56, Path: //bom[1]/components[1]/component[463]/externalReferences[1]/reference[1]/url[1], Error: cvc-type.3.1.3: The value 'git@github.com:lupomontero/psl.git' of element 'url' is not valid.");
    SbomDetectionResult expected =
        createExpectedResult(true, false, true, "application/xml", "Not a valid CycloneDX SBOM file.",
            expectedErrors, "1.4", "CycloneDx", "xml", 592, 24,
            "nodered/node-red", "sha256:337760fdb5d3d442185827379c33f6c414fbe5212fe2c108963d91d6c000318e");
    checkSbomMetadata("cyclonedx-invalid-2-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_InvalidJson() throws Exception {
    List<String> expectedErrors =
        List.of("Line: 1, Column: 2, Path: , Error: Missing required field \"dataLicense\".",
            "Line: 1, Column: 2, Path: , Error: Missing required field \"name\".");
    SbomDetectionResult expected =
        createExpectedResult(true, false, true, "application/json", "Not a valid SPDX SBOM file.",
            expectedErrors, "2.3", "SPDX", "json", 6, 13,
            "sonatype:iq_application_SCM Test 1", "76b10b862e7b42009f2415097620928c");
    checkSbomMetadata("spdx-invalid-json.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_InvalidJson_ignoreValidationError() throws Exception {
    SbomDetectionResult expected = createValidationIgnoredExpectedResult("application/json", "2.3", "SPDX", "json", 6,
        13, "sonatype:iq_application_SCM Test 1", "76b10b862e7b42009f2415097620928c");
    checkSbomMetadata("spdx-invalid-json.tmp", expected, true);
  }

  @Test
  public void testGetSbomMetadata_SPDX_InvalidJson_SKIP_SBOM_IMPORT_VALIDATION() throws Exception {
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);
    SbomDetectionResult expected = createValidationIgnoredExpectedResult("application/json", "2.3", "SPDX", "json", 6,
        13, "sonatype:iq_application_SCM Test 1", "76b10b862e7b42009f2415097620928c");
    checkSbomMetadata("spdx-invalid-json.tmp", expected, true);
  }

  @Test
  public void testGetSbomMetadata_SPDX_InvalidXml() throws Exception {
    SbomDetectionResult expected =
        createExpectedResult(true, false, false, "application/xml", "Not a valid SPDX SBOM file.",
            List.of("Error: Mismatched externalRefs and packages at 880 [character 12 line 21]"), null, "SPDX", "xml",
            0, 0, null, null);
    checkSbomMetadata("spdx-invalid-xml.tmp", expected);
  }

  @Test
  public void testGetSbomMetadata_Other_Binary() throws Exception {
    SbomDetectionResult expected = createBinaryExpectedResult("application/java-vm",
        "Provided file type is not a supported SBOM file type.");
    checkSbomMetadataUsingFile("test.bin", expected, false);
    checkSbomMetadataUsingFileWithGenericExtension("test.bin", expected, false);
  }

  @Test
  public void testGetSbomMetadata_Other_Text() throws Exception {
    SbomDetectionResult expected = createBinaryExpectedResult("text/plain",
        "Provided file type is not a supported SBOM file type.");
    checkSbomMetadata("test.tt", expected);
  }

  @Test
  public void testGetSbomMetadata_Other_Text_UnsafeContent_CycloneDx() throws Exception {
    Path fileToDetect = getTestPath("unsafe-plain-text-cdx.tt");
    String sbomContent = Files.readString(fileToDetect);
    assertThat(detector.isPlainTextValidXml(sbomContent)).isFalse();
  }

  @Test
  public void testGetSbomMetadata_Other_Text_UnsafeContent_SPDX() throws Exception {
    Path fileToDetect = getTestPath("unsafe-plain-text-spdx.tt");
    String sbomContent = Files.readString(fileToDetect);
    assertThat(detector.isPlainTextValidXml(sbomContent)).isFalse();
  }

  @Test
  public void testGetSbomMetadata_Other_Text_SafeContent() throws Exception {
    Path fileToDetect = getTestPath("safe-plain-text.tt");
    String sbomContent = Files.readString(fileToDetect);
    assertThat(detector.isPlainTextValidXml(sbomContent)).isTrue();
  }

  @Test
  public void testGetSbomMetadata_CDX_Json_BadStructure() throws Exception {
    List<String> expectedErrors = List.of("Error: Unable to parse BOM from byte array",
        "Line: 11, Column: 3, Error: Unexpected close marker ']': expected '}' (for Object starting at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 6, column: 2])");
    SbomDetectionResult expected1 =
        createExpectedResult(true, false, false, "application/json", "Not a valid CycloneDX SBOM file.",
            expectedErrors, null, "CycloneDx", "json", 0, 0, null, null);
    checkSbomMetadataUsingFile("cdx-bad-structure.json", expected1, false);
    SbomDetectionResult expected2 =
        createBinaryExpectedResult("text/plain", "Provided file type is not a supported SBOM file type.");
    checkSbomMetadataUsingFileWithGenericExtension("cdx-bad-structure.json", expected2, false);
  }

  @Test
  public void testGetSbomMetadata_CDX_Xml_BadStructure() throws Exception {
    List<String> expectedErrors = List.of(
        "Line: 9, Column: 1, Error: The end-tag for element type \"components\" must end with a '>' delimiter.");
    SbomDetectionResult expected =
        createExpectedResult(true, false, false, "application/xml", "Not a valid CycloneDX SBOM file.",
            expectedErrors, null, "CycloneDx", "xml", 0, 0, null, null);
    checkSbomMetadata("cdx-bad-structure.xml", expected);
  }

  @Test
  public void testGetSbomMetadata_SPDX_Json_BadStructure() throws Exception {
    List<String> expectedErrors = List.of(
        "Line: 20, Column: 3, Error: Unexpected close marker ']': expected '}' (for Object starting at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 15, column: 2])");
    SbomDetectionResult expected1 =
        createExpectedResult(true, false, false, "application/json", "Not a valid SPDX SBOM file.",
            expectedErrors, null, "SPDX", "json", 0, 0, null, null);
    checkSbomMetadataUsingFile("spdx-bad-structure.json", expected1, false);
    SbomDetectionResult expected2 =
        createBinaryExpectedResult("text/plain", "Provided file type is not a supported SBOM file type.");
    checkSbomMetadataUsingFileWithGenericExtension("spdx-bad-structure.json", expected2, false);
  }

  @Test
  public void testGetSbomMetadata_SPDX_Xml_BadStructure() throws Exception {
    List<String> expectedErrors = List.of("Error: Misplaced '<' at 606 [character 1 line 20]");
    SbomDetectionResult expected =
        createExpectedResult(true, false, false, "application/xml", "Not a valid SPDX SBOM file.",
            expectedErrors, null, "SPDX", "xml", 0, 0, null, null);
    checkSbomMetadata("spdx-bad-structure.xml", expected);
  }

  @Test
  public void testGetSbomMetadata_Json_BadStructure() throws Exception {
    SbomDetectionResult expected1 =
        createBinaryExpectedResult("application/json", "Not a valid/supported SBOM file.");
    checkSbomMetadataUsingFile("bad-structure.json", expected1, false);
    SbomDetectionResult expected2 =
        createBinaryExpectedResult("text/plain", "Provided file type is not a supported SBOM file type.");
    checkSbomMetadataUsingFileWithGenericExtension("bad-structure.json", expected2, false);
  }

  @Test
  public void testGetSbomMetadata_Xml_BadStructure() throws Exception {
    SbomDetectionResult expected =
        createBinaryExpectedResult("application/xml", "Not a valid/supported SBOM file.");
    checkSbomMetadata("bad-structure.xml", expected);
  }

  private void checkSbomMetadataUsingFile(
      String fileName,
      SbomDetectionResult expected,
      boolean ignoreValidationError) throws URISyntaxException
  {
    Path fileToDetect = getTestPath(fileName);
    SbomDetectionResult resultFromFile = detector.getSbomDetectionResult(fileToDetect, fileName, ignoreValidationError);

    verifySbomDetectionResult(resultFromFile, expected);
  }

  private void checkSbomMetadataUsingString(
      String fileName,
      SbomDetectionResult expected,
      boolean ignoreValidationError) throws Exception
  {
    Path fileToDetect = getTestPath(fileName);
    SbomDetectionResult resultFromFile =
        detector.getSbomDetectionResult(Files.readString(fileToDetect), fileName, ignoreValidationError);

    verifySbomDetectionResult(resultFromFile, expected);
  }

  private void checkSbomMetadataUsingFileWithGenericExtension(
      String fileName,
      SbomDetectionResult expected,
      boolean ignoreValidationError) throws Exception
  {
    Path fileToDetect = copyFileWithGenericExtension(getTestPath(fileName));
    SbomDetectionResult resultFromFile = detector.getSbomDetectionResult(fileToDetect, fileName, ignoreValidationError);

    verifySbomDetectionResult(resultFromFile, expected);
  }

  private void checkSbomMetadata(
      String fileName,
      SbomDetectionResult expected,
      boolean ignoreValidationError) throws Exception
  {
    checkSbomMetadataUsingFile(fileName, expected, ignoreValidationError);
    checkSbomMetadataUsingString(fileName, expected, ignoreValidationError);
    checkSbomMetadataUsingFileWithGenericExtension(fileName, expected, ignoreValidationError);
  }

  private void checkSbomMetadata(String fileName, SbomDetectionResult expected) throws Exception {
    checkSbomMetadata(fileName, expected, false);
  }

  private Path copyFileWithGenericExtension(Path file) throws Exception {
    String filename = file.getFileName().toString();
    Path target = tempDir.newFile(filename.substring(0, filename.lastIndexOf('.')) + ".tmp").toPath();
    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
    return target;
  }

  private static SbomDetectionResult createValidSbomExpectedResult(
      String mimeType,
      String version,
      String specification,
      String format,
      int componentCount,
      int vulnerabilityCount,
      String applicationName,
      String applicationVersion)
  {
    return createExpectedResult(true, true, null, mimeType, null, null, version, specification, format,
        componentCount, vulnerabilityCount, applicationName, applicationVersion);
  }

  private static SbomDetectionResult createValidationIgnoredExpectedResult(
      String mimeType,
      String version,
      String specification,
      String format,
      int componentCount,
      int vulnerabilityCount,
      String applicationName,
      String applicationVersion)
  {
    return createExpectedResult(true, false, true, mimeType, null, null, version, specification, format,
        componentCount, vulnerabilityCount, applicationName, applicationVersion);
  }

  private static SbomDetectionResult createInvalidSbomExpectedResult(
      String mimeType,
      String errorMessage,
      List<String> errors,
      Boolean isValidationErrorIgnorable)
  {
    return createExpectedErrorResult(true, false, isValidationErrorIgnorable, mimeType, errorMessage, errors);
  }

  private static SbomDetectionResult createBinaryExpectedResult(String mimeType, String errorMessage) {
    return createExpectedErrorResult(false, null, null, mimeType, errorMessage, null);
  }

  private static SbomDetectionResult createExpectedErrorResult(
      boolean isSbom,
      Boolean isValid,
      Boolean isValidationErrorIgnorable,
      String mimeType,
      String errorMessage,
      List<String> errors)
  {
    return createExpectedResult(isSbom, isValid, isValidationErrorIgnorable, mimeType, errorMessage, errors,
        null, null, null, 0, 0, null, null);
  }

  private static SbomDetectionResult createExpectedResult(
      boolean isSbom,
      Boolean isValid,
      Boolean isValidationErrorIgnorable,
      String mimeType,
      String errorMessage,
      List<String> errors,
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
    expected.isValid = isValid;
    expected.isValidationErrorIgnorable = isValidationErrorIgnorable;
    expected.mimeType = mimeType;
    if (errorMessage != null) {
      expected.errorMessage = errorMessage;
      expected.validationErrors = errors;
      if (errors != null && (version != null || specification != null || format != null)) {
        expected.summary = new SbomSummary();
        expected.summary.version = version;
        expected.summary.specification = specification;
        expected.summary.format = format;
      }
    }
    if (expected.isSbom && !Boolean.FALSE.equals(isValidationErrorIgnorable)) {
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
    assertThat(result.isValid).isEqualTo(expected.isValid);
    assertThat(result.isValidationErrorIgnorable).isEqualTo(expected.isValidationErrorIgnorable);
    assertThat(result.mimeType).isEqualTo(expected.mimeType);
    assertThat(result.errorMessage).isEqualTo(expected.errorMessage);
    assertThat(result.validationErrors).isEqualTo(expected.validationErrors);
    if (expected.summary != null && result.summary != null) {
      assertThat(result.summary.version).isEqualTo(expected.summary.version);
      assertThat(result.summary.specification).isEqualTo(expected.summary.specification);
      assertThat(result.summary.format).isEqualTo(expected.summary.format);
      assertThat(result.summary.componentCount).isEqualTo(expected.summary.componentCount);
      assertThat(result.summary.vulnerabilityCount).isEqualTo(expected.summary.vulnerabilityCount);
      assertThat(result.summary.applicationName).isEqualTo(expected.summary.applicationName);
      assertThat(result.summary.applicationVersion).isEqualTo(expected.summary.applicationVersion);
    }
    else {
      assertThat(result.summary).isEqualTo(expected.summary);
    }
  }

  private Path getTestPath(final String fileName) throws URISyntaxException {
    URL resource = SbomFileDetectorTest.class.getResource("/SbomFileDetectorTest/" + fileName);
    return Paths.get(Objects.requireNonNull(resource).toURI());
  }
}
