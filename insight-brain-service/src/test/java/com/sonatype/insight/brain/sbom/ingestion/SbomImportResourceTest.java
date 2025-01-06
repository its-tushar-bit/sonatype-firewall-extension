/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomImportResourceTest
    extends AbstractResourceTest
{
  private Application application;

  private PolicyEvaluationHelper policyEvaluationHelper;

  @Before
  public void before() throws Exception {

    licenseManager.setProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER
    );

    installLicense();

    application = tempEntity.newApplicationWithParent();

    policyEvaluationHelper = lookup(PolicyEvaluationHelper.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SbomImportResource.RESOURCE_PATH);
  }

  @Test
  public void testDetectSbom_ValidCycloneDxSbom() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/valid-cyclonedx-bom.xml");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();
    assertResponseStatus(200, response);
    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().applicationName).isEqualTo("iq_application_vuln");
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo("a140fd3c3ded4bb0a640dc31e2904dc9");
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(1);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(1);
    assertThat(actual.getSbomSummary().specification).isEqualTo("CycloneDx");
    assertThat(actual.getSbomSummary().format).isEqualTo("xml");
    assertThat(actual.getSbomSummary().version).isEqualTo("1.5");
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getIsValid()).isTrue();
    assertThat(actual.getIsValidationErrorIgnorable()).isNull();
  }

  @Test
  public void testDetectSbom_ValidSpdxSbom() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/valid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();
    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertResponseStatus(200, response);
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().applicationName).isEqualTo("sonatype:iq_application_vuln");
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo("a140fd3c3ded4bb0a640dc31e2904dc9");
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(2);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(1);
    assertThat(actual.getSbomSummary().specification).isEqualTo("SPDX");
    assertThat(actual.getSbomSummary().format).isEqualTo("json");
    assertThat(actual.getSbomSummary().version).isEqualTo("2.3");
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getIsValid()).isTrue();
    assertThat(actual.getIsValidationErrorIgnorable()).isNull();
  }

  @Test
  public void testDetectBinary() throws Exception {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/binary.jar");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();
    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertResponseStatus(200, response);
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(actual.getIsValid()).isNull();
    assertThat(actual.getIsValidationErrorIgnorable()).isNull();
  }

  @Test
  public void testDetectSbom_InvalidCycloneDxSbom() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/invalid-cyclonedx-bom.xml");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .query("ignoreValidationError", false)
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();

    assertResponseStatus(200, response);
    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isTrue();
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid CycloneDX SBOM file.");
  }

  @Test
  public void testDetectSbom_InvalidSpdxSbom() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/invalid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .query("ignoreValidationError", false)
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();

    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertResponseStatus(200, response);
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isTrue();
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid SPDX SBOM file.");
  }

  @Test
  public void testDetectSbom_InvalidCycloneDxSbomStructure() throws Exception {
    URL resource =
        SbomImportResourceTest.class.getResource("/SbomImportResourceTest/invalid-cyclonedx-bom-structure.xml");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .query("ignoreValidationError", true)
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();

    assertResponseStatus(200, response);
    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isFalse();
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid CycloneDX SBOM file.");
  }

  @Test
  public void testDetectSbom_InvalidSpdxSbomStructure() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/invalid-spdx-bom-structure.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .query("ignoreValidationError", true)
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();

    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertResponseStatus(200, response);
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isFalse();
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid SPDX SBOM file.");
  }

  @Test
  public void testDetectSbom_InvalidCycloneDxSbom_IgnoreValidationError() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/invalid-cyclonedx-bom.xml");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .query("ignoreValidationError", true)
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();

    assertResponseStatus(200, response);
    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isTrue();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().applicationName).isEqualTo("iq_application_vuln");
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo("a140fd3c3ded4bb0a640dc31e2904dc9");
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(1);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(1);
    assertThat(actual.getSbomSummary().specification).isEqualTo("CycloneDx");
    assertThat(actual.getSbomSummary().format).isEqualTo("xml");
    assertThat(actual.getSbomSummary().version).isEqualTo("1.5");
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
  }

  @Test
  public void testDetectSbom_InvalidSpdxSbom_IgnoreValidationError() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/invalid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .query("ignoreValidationError", true)
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();

    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertResponseStatus(200, response);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isTrue();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().applicationName).isEqualTo("sonatype:iq_application_SCM Test 1");
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo("76b10b862e7b42009f2415097620928c");
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(6);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(5);
    assertThat(actual.getSbomSummary().specification).isEqualTo("SPDX");
    assertThat(actual.getSbomSummary().format).isEqualTo("json");
    assertThat(actual.getSbomSummary().version).isEqualTo("2.3");
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
  }

  @Test
  public void testDetectSbom_InvalidApplicationId() throws Exception {
    HttpResponse response = restRequest()
        .parameter("applicationId")
        .part("file", "sbom.xml", new Byte[1])
        .path(SbomImportResource.DETECT_PATH)
        .post();
    assertResponseStatus(404, response);
  }

  @Test
  public void testImportDetectedSbom_InvalidApplicationId() throws Exception {
    HttpResponse response = restRequest()
        .path(SbomImportResource.COMMIT_PATH)
        .parameter("applicationId", "requestId")
        .post();

    assertResponseStatus(404, response);
  }

  @Test
  public void testImportDetectedSbom_InvalidApplicationVersion() throws Exception {
    mockHdsReportDownload();

    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/valid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    restRequest()
        .parameter(application.getId())
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();

    HttpResponse importResponse = restRequest()
        .path(SbomImportResource.COMMIT_PATH)
        .parameter(application.getId(), "invalidVersion")
        .post();

    assertResponseStatus(404, importResponse);
  }

  @Test
  public void testImportDetectedSbom_BINARY_Success() throws Exception {
    testImportDetectedSbom_Success("binary.jar", false);
  }

  @Test
  public void testImportDetectedSbom_SBOM_Success() throws Exception {
    testImportDetectedSbom_Success("valid-spdx-bom.json", false);
  }

  @Test
  public void testImportDetectedSbom_SkipSpdxSbomValidation_Success() throws Exception {
    testImportDetectedSbom_Success("invalid-spdx-bom.json", true);
  }

  @Test
  public void testImportDetectedSbom_SkipCycloneDxSbomValidation_Success() throws Exception {
    testImportDetectedSbom_Success("invalid-cyclonedx-bom.xml", true);
  }

  private void testImportDetectedSbom_Success(String fileName, boolean ignoreValidationError) throws Exception {
    mockHdsReportDownload();

    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/" + fileName);
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse responseDetect = restRequest()
        .parameter(application.getId())
        .query("ignoreValidationError", ignoreValidationError)
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();
    SbomDetectionResultDTO actual = responseDetect.getBody(SbomDetectionResultDTO.class);

    HttpResponse responseCommit = restRequest()
        .path(SbomImportResource.COMMIT_PATH)
        .parameter(application.getId(), actual.getSavedVersion())
        .post();

    ApiThirdPartyScanTicketDTO responseCommitBody = responseCommit.getBody(ApiThirdPartyScanTicketDTO.class);

    assertResponseStatus(202, responseCommit);
    assertThat(responseCommitBody).isNotNull();
    assertThat(responseCommitBody.statusUrl).isNotEmpty();
    assertThat(responseCommitBody.statusUrl).startsWith("api/v2/sbom/applications/" + application.getId() + "/status/");

    policyEvaluationHelper.awaitEvaluationCompleted(application.getId(), getStatusId(responseCommitBody.statusUrl));
  }

  private String getStatusId(String statusUrl) {
    return statusUrl.substring(statusUrl.lastIndexOf("/") + 1);
  }

  private void mockHdsReportDownload() {
    URL resourceUrl = ReportHelper.zipReport("/ReportServiceTest/report-with-dependencies", tempDir);
    hdsRespondWith(resourceUrl).atUri("rest/application/analysis/SCAN-ID");
  }
}
