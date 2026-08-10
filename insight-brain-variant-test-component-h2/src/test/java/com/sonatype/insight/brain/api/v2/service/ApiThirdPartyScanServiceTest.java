/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.v2.dto.ApiEvaluationResultCounterDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.service.ApiThirdPartyScanService.ApiPolicyAction;
import com.sonatype.insight.brain.dataaccess.NotAcceptableException;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultDTO;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.file.SbomFormat;
import jakarta.inject.Inject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ApiThirdPartyScanServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiThirdPartyScanService thirdPartyScanService;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  private Application app;

  @Mock
  private PolicyEvaluateService mockPolicyEvaluateService;

  @BeforeEach
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testScanComponents_bom_v1_0() throws Exception {
    testScanComponentsWithFailure("valid_bom_1_0.xml", SbomFormat.XML, "CycloneDX XML 1.0 version is not supported");
  }

  @Test
  public void testScanComponents_bom_v1_1() throws Exception {
    testScanComponents("valid_bom.xml", SbomFormat.XML);
  }

  @Test
  public void testScanComponents_bom_v1_2() throws Exception {
    testScanComponents("valid_bom_1_2.xml", SbomFormat.XML);
  }

  @Test
  public void testScanComponents_bom_v1_3() throws Exception {
    testScanComponents("valid_bom_1_3.xml", SbomFormat.XML);
  }

  @Test
  public void testScanComponents_invalidBom_v1_3_skipSbomValidationEnabled() throws Exception {
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);
    testScanComponents("invalid_bom_1_3.xml", SbomFormat.XML);
  }

  @Test
  public void testScanComponents_bom_json() throws Exception {
    testScanComponents("valid_bom.json", SbomFormat.JSON);
  }

  @Test
  public void testScanComponents_spdx_2_2_json_invalid() throws Exception {
    testScanComponentsWithFailure("invalid_spdx_2_2.json", SbomFormat.JSON,
        "The sbom is not valid.\n - Line: 37, Column: 6, Path: /packages/1, " +
            "Error: Missing required field \"copyrightText\".");
  }

  @Test
  public void testScanComponents_spdx_2_2_xml_invalid() throws Exception {
    testScanComponentsWithFailure("invalid_spdx_2_2.xml", SbomFormat.XML,
        "The sbom is not valid.\n - Error: Missing required download location for package log4j:log4j in" +
            " sonatype:iq_application_webgoat in sonatype:iq_application_webgoat in webgoat");
  }

  @Test
  public void testScanComponents_spdx_2_3_json_invalid() throws Exception {
    testScanComponentsWithFailure("invalid_spdx_2_3.json", SbomFormat.JSON,
        "The sbom is not valid.\n - Line: 1, Column: 2, Path: , Error: Missing required field \"name\".");
  }

  @Test
  public void testScanComponents_spdx_2_3_xml_invalid() throws Exception {
    testScanComponentsWithFailure("invalid_spdx_2_3.xml", SbomFormat.XML,
        "The sbom is not valid.\n - Error: Missing required creators");
  }

  @Test
  public void testScanComponents_spdx_2_2_json() throws Exception {
    testScanComponents("valid_spdx_2_2.json", SbomFormat.JSON);
  }

  @Test
  public void testScanComponents_spdx_2_2_xml() throws Exception {
    testScanComponents("valid_spdx_2_2.xml", SbomFormat.XML);
  }

  @Test
  public void testScanComponents_spdx_2_3_json() throws Exception {
    testScanComponents("valid_spdx_2_3.json", SbomFormat.JSON);
  }

  @Test
  public void testScanComponents_spdx_2_3_xml() throws Exception {
    testScanComponents("valid_spdx_2_3.xml", SbomFormat.XML);
  }

  public void testScanComponentsWithFailure(
      String fileName,
      SbomFormat format,
      String expectedMessage) throws Exception
  {
    String bom = getBomFile(fileName);
    String appId = app.getId();
    assertThatExceptionOfType(NotAcceptableException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "source", Stage.ID_BUILD, bom, null,
            format))
        .withMessage(expectedMessage);
  }

  public void testScanComponents(String fileName, SbomFormat format) throws Exception {
    String bom = getBomFile(fileName);

    ApiThirdPartyScanTicketDTO scanResult =
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null, format);
    assertThat(scanResult).isNotNull();
    assertThat(scanResult.statusUrl).isNotNull();
    assertThat(new URI(scanResult.statusUrl)).isNotNull();
  }

  @Test
  public void testGetScanStatus_Pending() {
    PolicyEvaluationPollingResultDTO policyEvaluationPollingResult = new PolicyEvaluationPollingResultDTO();
    policyEvaluationPollingResult.status = PolicyEvaluationStatus.PENDING;

    when(mockPolicyEvaluateService.pollEvaluationResult(
        argThat((Application a) -> a != null && app.getId().equals(a.getId())), eq("scanId")))
            .thenReturn(policyEvaluationPollingResult);

    assertThatThrownBy(() -> thirdPartyScanService.getScanStatus(app.getId(), "scanId"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            String.format("Report with status id %s for application with id %s is not ready.", "scanId", app.getId()));
  }

  @Test
  public void testGetScanStatus_Failure() {
    PolicyEvaluationPollingResultDTO policyEvaluationPollingResult = new PolicyEvaluationPollingResultDTO();
    policyEvaluationPollingResult.status = PolicyEvaluationStatus.FAILED;
    policyEvaluationPollingResult.reason = "HDS Upload Failure!";

    when(mockPolicyEvaluateService.pollEvaluationResult(
        argThat((Application a) -> a != null && app.getId().equals(a.getId())), eq("scanId")))
            .thenReturn(policyEvaluationPollingResult);

    ApiThirdPartyScanResultDTO thirdPartyScanResult = thirdPartyScanService.getScanStatus(app.getId(), "scanId");
    assertThat(thirdPartyScanResult.isError).isTrue();
    assertThat(thirdPartyScanResult.errorMessage).isEqualTo("HDS Upload Failure!");
  }

  @Test
  public void testGetScanStatus_Completed_PolicyAction_None() throws Exception {
    testGetScanStatus_Completed(null, "None");
  }

  @Test
  public void testGetScanStatus_Completed_PolicyAction_Warn() throws Exception {
    testGetScanStatus_Completed(Action.ID_WARN, "Warning");
  }

  @Test
  public void testGetScanStatus_Completed_PolicyAction_Fail() throws Exception {
    testGetScanStatus_Completed(Action.ID_FAIL, "Failure");
  }

  @Test
  public void testGetScanStatus_Completed_Counters_Xml() throws Exception {
    testGetScanStatus_Completed_Counters("valid_bom.xml");
  }

  @Test
  public void testGetScanStatus_Completed_Counters_Json() throws Exception {
    testGetScanStatus_Completed_Counters("valid_bom.json");
  }

  public void testGetScanStatus_Completed_Counters(String fileName) throws Exception {
    ApiEvaluationResultCounterDTO componentsAffected = new ApiEvaluationResultCounterDTO();
    componentsAffected.critical = 1;
    componentsAffected.moderate = 3;
    componentsAffected.severe = 4;
    ApiEvaluationResultCounterDTO openPolicyViolations = new ApiEvaluationResultCounterDTO();
    openPolicyViolations.critical = 0;
    openPolicyViolations.moderate = 1;
    openPolicyViolations.severe = 10;
    testGetScanStatus_Completed(null, "None", componentsAffected, openPolicyViolations, 5, fileName);
  }

  private void testGetScanStatus_Completed(String actionId, String policyAction) throws Exception {
    ApiEvaluationResultCounterDTO componentsAffected = new ApiEvaluationResultCounterDTO();
    componentsAffected.critical = 0;
    componentsAffected.moderate = 0;
    componentsAffected.severe = 0;
    ApiEvaluationResultCounterDTO openPolicyViolations = new ApiEvaluationResultCounterDTO();
    openPolicyViolations.critical = 0;
    openPolicyViolations.moderate = 0;
    openPolicyViolations.severe = 0;
    testGetScanStatus_Completed(actionId, policyAction, componentsAffected, openPolicyViolations, 0, "valid_bom.xml");
  }

  private void testGetScanStatus_Completed(
      String actionId,
      String policyAction,
      ApiEvaluationResultCounterDTO componentsAffected,
      ApiEvaluationResultCounterDTO openPolicyViolations,
      Integer legacyViolations,
      String fileName) throws Exception
  {
    String scanId = "testScan";
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);

    Application app = tempEntity.newApplicationWithParent();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    scanReceipt.setReportUrl("link_to_report");
    scanReceipt.setPdfUrl("link_to_pdf");
    scanReceipt.setDataUrl("link_to_data");

    List<PolicyAlert> alerts = new ArrayList<>();
    if (actionId != null) {
      alerts.add(new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10),
          Collections.singletonList(new Action(actionId))));
    }

    PolicyEvaluationResult evaluationResult = new PolicyEvaluationResult();
    evaluationResult.setAlerts(alerts);
    evaluationResult.setCriticalComponentCount(componentsAffected.critical);
    evaluationResult.setModerateComponentCount(componentsAffected.moderate);
    evaluationResult.setSevereComponentCount(componentsAffected.severe);
    evaluationResult.setCriticalPolicyViolationCount(openPolicyViolations.critical);
    evaluationResult.setModeratePolicyViolationCount(openPolicyViolations.moderate);
    evaluationResult.setSeverePolicyViolationCount(openPolicyViolations.severe);
    evaluationResult.setLegacyViolationCount(legacyViolations);

    PolicyEvaluationPollingResultDTO pollingResult = new PolicyEvaluationPollingResultDTO();
    pollingResult.scanReceipt = scanReceipt;
    pollingResult.status = PolicyEvaluationStatus.COMPLETED;
    pollingResult.result = evaluationResult;

    when(mockPolicyEvaluateService.pollEvaluationResult(
        argThat((Application a) -> a != null && app.getId().equals(a.getId())), eq(scanId)))
            .thenReturn(pollingResult);

    String bom = getBomFile(fileName);

    thirdPartyScanService.scanComponents(app.getId(), "clair", Stage.ID_BUILD, bom, null, SbomFormat.XML);

    ApiThirdPartyScanResultDTO resultDTO = thirdPartyScanService.getScanStatus(app.getId(), scanId);
    assertThat(resultDTO.policyAction).isEqualTo(policyAction);
    assertThat(resultDTO.reportHtmlUrl).isEqualTo("link_to_report");
    assertThat(resultDTO.reportPdfUrl).isEqualTo("link_to_pdf");
    assertThat(resultDTO.reportDataUrl).isEqualTo("link_to_data");
    assertThat(resultDTO.embeddableReportHtmlUrl)
        .isEqualTo(String.format("ui/links/application/%s/report/%s/embeddable", app.getPublicId(), scanId));
    assertThat(resultDTO.isError).isFalse();
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.componentsAffected).isNotNull();
    assertThat(resultDTO.componentsAffected.critical).isEqualTo(componentsAffected.critical);
    assertThat(resultDTO.componentsAffected.moderate).isEqualTo(componentsAffected.moderate);
    assertThat(resultDTO.componentsAffected.severe).isEqualTo(componentsAffected.severe);
    assertThat(resultDTO.openPolicyViolations).isNotNull();
    assertThat(resultDTO.openPolicyViolations.critical).isEqualTo(openPolicyViolations.critical);
    assertThat(resultDTO.openPolicyViolations.moderate).isEqualTo(openPolicyViolations.moderate);
    assertThat(resultDTO.openPolicyViolations.severe).isEqualTo(openPolicyViolations.severe);
    assertThat(resultDTO.grandfatheredPolicyViolations).isEqualTo(legacyViolations);
    assertThat(resultDTO.legacyViolations).isEqualTo(legacyViolations);
  }

  private String getBomFile(String path) throws Exception {
    byte[] bytes =
        Files.readAllBytes(Paths.get(getClass().getResource("/" + getClass().getSimpleName() + "/" + path).toURI()));
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Test
  public void testApiPolicyAction_Combine_None() {
    assertApiPolicy(ApiPolicyAction.NONE, ApiPolicyAction.NONE, ApiPolicyAction.NONE);
    assertApiPolicy(ApiPolicyAction.NONE, ApiPolicyAction.WARN, ApiPolicyAction.WARN);
    assertApiPolicy(ApiPolicyAction.NONE, ApiPolicyAction.FAIL, ApiPolicyAction.FAIL);
  }

  @Test
  public void testApiPolicyAction_Combine_Warn() {
    assertApiPolicy(ApiPolicyAction.WARN, ApiPolicyAction.NONE, ApiPolicyAction.WARN);
    assertApiPolicy(ApiPolicyAction.WARN, ApiPolicyAction.WARN, ApiPolicyAction.WARN);
    assertApiPolicy(ApiPolicyAction.WARN, ApiPolicyAction.FAIL, ApiPolicyAction.FAIL);
  }

  @Test
  public void testApiPolicyAction_Combine_Fail() {
    assertApiPolicy(ApiPolicyAction.FAIL, ApiPolicyAction.NONE, ApiPolicyAction.FAIL);
    assertApiPolicy(ApiPolicyAction.FAIL, ApiPolicyAction.WARN, ApiPolicyAction.FAIL);
    assertApiPolicy(ApiPolicyAction.FAIL, ApiPolicyAction.FAIL, ApiPolicyAction.FAIL);
  }

  @Test
  public void testScanComponents_invalid_vulnerability_id() throws Exception {
    String bom = getBomFile("invalid_bom_id_vulnerability.xml");

    ApiThirdPartyScanTicketDTO scanResult =
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null, SbomFormat.XML);
    assertThat(scanResult).isNotNull();
    assertThat(scanResult.statusUrl).isNotNull();
    assertThat(new URI(scanResult.statusUrl)).isNotNull();
  }

  @Test
  public void testScanComponents_invalid_vulnerability_score_base() throws Exception {
    String bom = getBomFile("invalid_bom_base_score_vulnerability.xml");

    ApiThirdPartyScanTicketDTO scanResult =
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null, SbomFormat.XML);
    assertThat(scanResult).isNotNull();
    assertThat(scanResult.statusUrl).isNotNull();
    assertThat(new URI(scanResult.statusUrl)).isNotNull();
  }

  @Test
  public void testScanComponents_StageNotLicensed() {
    productLicenseManager.setStageTypes(StageTypes.RELEASE);
    String appId = app.getId();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "clair", Stage.ID_BUILD, "bom", null,
            SbomFormat.XML))
        .withMessage("Stage 'build' is not supported by your license.");
  }

  @Test
  public void testScanComponents_NullBom() {
    String appId = app.getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "clair", Stage.ID_BUILD, null, null,
            SbomFormat.XML))
        .withMessage("sbom content is null or empty");
  }

  @Test
  public void testScanComponents_InvalidStage() throws Exception {
    String bom = getBomFile("invalid_bom.xml");
    String appId = app.getId();
    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "clair", "invalidStage", bom, null,
            SbomFormat.XML))
        .withMessage("Invalid stage id=invalidStage");
  }

  @Test
  public void testScanComponents_Invalid_Content_Xml_v1_1() throws Exception {
    testScanComponents_Invalid_Content("invalid_bom.xml");
  }

  @Test
  public void testScanComponents_Invalid_Content_Xml_v1_2() throws Exception {
    testScanComponents_Invalid_Content("invalid_bom_1_2.xml");
  }

  @Test
  public void testScanComponents_Invalid_Content_Xml_v1_3() throws Exception {
    testScanComponents_Invalid_Content("invalid_bom_1_3.xml");
  }

  @Test
  public void testScanComponents_Invalid_Content_Json() throws Exception {
    String bom = getBomFile("invalid_bom.json");
    String appId = app.getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "clair", "build", bom, null,
            SbomFormat.JSON))
        .withMessage("SBOM content cannot be parsed.");
  }

  @Test
  public void testScanComponents_Invalid_Content_Xml() throws Exception {
    String bom = getBomFile("invalid_xml_bom.xml");
    String appId = app.getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "clair", "build", bom, null,
            SbomFormat.XML))
        .withMessage("SBOM content cannot be parsed.");
  }

  private void testScanComponents_Invalid_Content(String fileName) throws Exception {
    String bom = getBomFile(fileName);
    ApiThirdPartyScanTicketDTO scanResult =
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null, SbomFormat.XML);
    assertThat(scanResult).isNotNull();
    assertThat(scanResult.statusUrl).isNotNull();
    assertThat(new URI(scanResult.statusUrl)).isNotNull();
  }

  private void assertApiPolicy(ApiPolicyAction action1, ApiPolicyAction action2, ApiPolicyAction result) {
    assertThat(action1.combine(action2)).isEqualTo(result);
  }
}
