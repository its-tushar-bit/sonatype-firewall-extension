/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
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
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.thirdparty.ThirdPartyUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class ApiThirdPartyScanServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiThirdPartyScanService thirdPartyScanService;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  private Application app;

  @Mock
  private PolicyEvaluateService mockPolicyEvaluateService;

  @Override
  public void configure(Binder binder) {
    binder.bind(PolicyEvaluateService.class).toInstance(mockPolicyEvaluateService);

    super.configure(binder);
  }

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testScanComponents_bom_v1_0() throws Exception {
    String bom = getBomFile("valid_bom_1_0.xml");
    String appId = app.getId();
    assertThatExceptionOfType(NotAcceptableException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "clair", Stage.ID_BUILD, bom, null,
            ThirdPartyUtils.XML_SBOM))
        .withMessage("CycloneDX XML 1.0 version is not supported");
  }

  @Test
  public void testScanComponents_bom_v1_1() throws Exception {
    testScanComponents("valid_bom.xml");
  }

  @Test
  public void testScanComponents_bom_v1_2() throws Exception {
    testScanComponents("valid_bom_1_2.xml");
  }

  @Test
  public void testScanComponents_bom_v1_3() throws Exception {
    testScanComponents("valid_bom_1_3.xml");
  }

  @Test
  public void testScanComponents_bom_json() throws Exception {
    testScanComponents("valid_bom.json");
  }

  public void testScanComponents(String fileName)
      throws Exception
  {
    String bom = getBomFile(fileName);

    ApiThirdPartyScanTicketDTO scanResult =
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null, ThirdPartyUtils.XML_SBOM);
    assertThat(scanResult).isNotNull();
    assertThat(scanResult.statusUrl).isNotNull();
    assertThat(new URI(scanResult.statusUrl)).isNotNull();
  }

  @Test
  public void testGetScanStatus_Pending() {
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.PENDING);

    when(mockPolicyEvaluateService.pollEvaluationResult(app.getPublicId(), "scanId"))
        .thenReturn(policyEvaluationPollingResult);

    assertThatThrownBy(() -> thirdPartyScanService.getScanStatus(app.getId(), "scanId"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            String.format("Report with status id %s for application with id %s is not ready.", "scanId", app.getId()));
  }

  @Test
  public void testGetScanStatus_Failure() {
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.FAILED);
    policyEvaluationPollingResult.setReason("HDS Upload Failure!");

    when(mockPolicyEvaluateService.pollEvaluationResult(app.getPublicId(), "scanId"))
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
      Integer grandfatheredPolicyViolations,
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
    evaluationResult.setGrandfatheredPolicyViolationCount(grandfatheredPolicyViolations);

    PolicyEvaluationPollingResult pollingResult = new PolicyEvaluationPollingResult();
    pollingResult.setScanReceipt(scanReceipt);
    pollingResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    pollingResult.setResult(evaluationResult);

    when(mockPolicyEvaluateService.pollEvaluationResult(app.getPublicId(), scanId))
        .thenReturn(pollingResult);

    String bom = getBomFile(fileName);

    thirdPartyScanService.scanComponents(app.getId(), "clair", Stage.ID_BUILD, bom, null, ThirdPartyUtils.XML_SBOM);

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
    assertThat(resultDTO.grandfatheredPolicyViolations).isEqualTo(grandfatheredPolicyViolations);
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
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null, ThirdPartyUtils.XML_SBOM);
    assertThat(scanResult).isNotNull();
    assertThat(scanResult.statusUrl).isNotNull();
    assertThat(new URI(scanResult.statusUrl)).isNotNull();
  }

  @Test
  public void testScanComponents_invalid_vulnerability_score_base() throws Exception {
    String bom = getBomFile("invalid_bom_base_score_vulnerability.xml");

    ApiThirdPartyScanTicketDTO scanResult =
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null, ThirdPartyUtils.XML_SBOM);
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
            ThirdPartyUtils.XML_SBOM))
        .withMessage("Stage 'build' is not supported by your license.");
  }

  @Test
  public void testScanComponents_NullBom() {
    String appId = app.getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "clair", Stage.ID_BUILD, null, null,
            ThirdPartyUtils.XML_SBOM))
        .withMessage("sbom content is null or empty");
  }

  @Test
  public void testScanComponents_InvalidStage() throws Exception {
    String bom = getBomFile("invalid_bom.xml");
    String appId = app.getId();
    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "clair", "invalidStage", bom, null,
            ThirdPartyUtils.XML_SBOM))
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
            ThirdPartyUtils.JSON_SBOM))
        .withMessage("sbom content cannot be parsed");
  }

  @Test
  public void testScanComponents_Invalid_Content_Xml() throws Exception {
    String bom = getBomFile("invalid_xml_bom.xml");
    String appId = app.getId();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> thirdPartyScanService.scanComponents(appId, "clair", "build", bom, null,
            ThirdPartyUtils.XML_SBOM))
        .withMessage("sbom content cannot be parsed");
  }

  private void testScanComponents_Invalid_Content(String fileName) throws Exception {
    String bom = getBomFile(fileName);
    ApiThirdPartyScanTicketDTO scanResult =
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null, ThirdPartyUtils.XML_SBOM);
    assertThat(scanResult).isNotNull();
    assertThat(scanResult.statusUrl).isNotNull();
    assertThat(new URI(scanResult.statusUrl)).isNotNull();
  }

  private void assertApiPolicy(ApiPolicyAction action1, ApiPolicyAction action2, ApiPolicyAction result) {
    assertThat(action1.combine(action2)).isEqualTo(result);
  }
}
