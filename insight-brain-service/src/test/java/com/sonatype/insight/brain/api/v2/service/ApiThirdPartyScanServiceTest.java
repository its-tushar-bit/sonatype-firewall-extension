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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
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

  @Inject
  private InsightConfig insightConfig;

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
  public void testScanComponents()
      throws Exception
  {
    String bom = getBomFile("valid_bom.xml");

    ApiThirdPartyScanTicketDTO scanResult =
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null);
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
  public void testGetScanStatus_Completed_Counters() throws Exception {
    ApiEvaluationResultCounterDTO componentsAffected = new ApiEvaluationResultCounterDTO();
    componentsAffected.critical = 1;
    componentsAffected.moderate = 3;
    componentsAffected.severe = 4;
    ApiEvaluationResultCounterDTO openPolicyViolations = new ApiEvaluationResultCounterDTO();
    openPolicyViolations.critical = 0;
    openPolicyViolations.moderate = 1;
    openPolicyViolations.severe = 10;
    testGetScanStatus_Completed(null, "None", componentsAffected, openPolicyViolations, 5);
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
    testGetScanStatus_Completed(actionId, policyAction, componentsAffected, openPolicyViolations, 0);
  }

  private void testGetScanStatus_Completed(
      String actionId,
      String policyAction,
      ApiEvaluationResultCounterDTO componentsAffected,
      ApiEvaluationResultCounterDTO openPolicyViolations,
      Integer grandfatheredPolicyViolations) throws Exception
  {
    String scanId = "testScan";
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);

    Application app = tempEntity.newApplicationWithParent();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    scanReceipt.setReportUrl("/link_to_report");

    List<PolicyAlert> alerts = new ArrayList<>();
    if (actionId != null) {
      alerts.add(new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10), asList(new Action(actionId))));
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
    insightConfig.setBaseUrl("http://iq.server");

    String bom = getBomFile("valid_bom.xml");

    thirdPartyScanService.scanComponents(app.getId(), "clair", Stage.ID_BUILD, bom, null);

    ApiThirdPartyScanResultDTO resultDTO = thirdPartyScanService.getScanStatus(app.getId(), scanId);
    assertThat(resultDTO.policyAction).isEqualTo(policyAction);
    assertThat(resultDTO.reportHtmlUrl).isEqualTo("http://iq.server/link_to_report");
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

    assertThatThrownBy(() -> thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null))
        .isInstanceOf(BadRequestException.class).hasMessage(
        "Error in component jackson-databind: An element <id> of vulnerability with " +
            "ref pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9 is null or empty");
  }

  @Test
  public void testScanComponents_invalid_vulnerability_score_base() throws Exception {
    String bom = getBomFile("invalid_bom_base_score_vulnerability.xml");

    assertThatThrownBy(() -> thirdPartyScanService.scanComponents(app.getId(), "clair", "build", bom, null))
        .isInstanceOf(BadRequestException.class).hasMessage(
        "Error in component jackson-databind: An element <base> of a vulnerability score with " +
            "ref pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9 is null or empty");
  }

  @Test
  public void testScanComponents_StageNotLicensed() {
    productLicenseManager.setStageTypes(StageTypes.RELEASE);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      thirdPartyScanService.scanComponents(app.getId(), "clair", Stage.ID_BUILD, "bom", null);
    }).withMessage("Stage 'build' is not supported by your license.");
  }

  @Test
  public void testScanComponents_InvalidBom() throws Exception {
    String bom = getBomFile("invalid_bom.xml");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      thirdPartyScanService.scanComponents(app.getId(), "clair", Stage.ID_BUILD, bom, null);
    }).withMessageMatching("Error on line number: 14, column number: 21 message: "
        + "cvc-complex-type.4:.*['\"]name['\"].*['\"]v:source['\"].*");
  }

  @Test
  public void testScanComponents_NullBom() throws Exception {
    String bom = null;

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      thirdPartyScanService.scanComponents(app.getId(), "clair", Stage.ID_BUILD, bom, null);
    }).withMessage("sbom content is null or empty");
  }

  @Test
  public void testScanComponents_InvalidStage() throws Exception {
    String bom = getBomFile("invalid_bom.xml");

    assertThatExceptionOfType(InvalidStageException.class).isThrownBy(() -> {
      thirdPartyScanService.scanComponents(app.getId(), "clair", "invalidStage", bom, null);
    }).withMessage("Invalid stage id=invalidStage");
  }

  private void assertApiPolicy(ApiPolicyAction action1, ApiPolicyAction action2, ApiPolicyAction result) {
    assertThat(action1.combine(action2)).isEqualTo(result);
  }
}
