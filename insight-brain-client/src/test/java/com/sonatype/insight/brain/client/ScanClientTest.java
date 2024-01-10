/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.signature.ComponentWithSignatures;
import com.sonatype.clm.dto.model.signature.ComponentWithSignaturesList;
import com.sonatype.clm.dto.model.signature.FunctionSignature;
import com.sonatype.clm.dto.model.signature.Signature;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ScanClientTest
    extends AbstractBrainServiceIntegrationTest
{
  private static final String APP_ID = "ScanClientTest_AppId";

  private static final int CRITICAL_COMPONENT_COUNT = 5;

  private static final int SEVERE_COMPONENT_COUNT = 4;

  private static final int MODERATE_COMPONENT_COUNT = 3;

  private static final int CRITICAL_POLICY_VIOLATION_COUNT = 9;

  private static final int SEVERE_POLICY_VIOLATION_COUNT = 8;

  private static final int MODERATE_POLICY_VIOLATION_COUNT = 7;

  private static final int LEGACY_VIOLATION_COUNT = 50;

  private ApplicationDAO applicationDAO;

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Before
  public void createApplication() {
    applicationDAO = lookup(ApplicationDAO.class);

    tempEntity.newApplicationWithParent(APP_ID, "test");
  }

  @Test
  public void testSaveResultData() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("the-scan-id");
    receipt.setReportUrl("the-report-url");
    receipt.setPdfUrl("the-pdf-url");
    receipt.setDataUrl("the-data-url");
    PolicyEvaluationResult evaluationResult = new PolicyEvaluationResult();
    evaluationResult.setAlerts(Collections.emptyList());
    evaluationResult.setCriticalComponentCount(CRITICAL_COMPONENT_COUNT);
    evaluationResult.setSevereComponentCount(SEVERE_COMPONENT_COUNT);
    evaluationResult.setModerateComponentCount(MODERATE_COMPONENT_COUNT);
    evaluationResult.setCriticalPolicyViolationCount(CRITICAL_POLICY_VIOLATION_COUNT);
    evaluationResult.setSeverePolicyViolationCount(SEVERE_POLICY_VIOLATION_COUNT);
    evaluationResult.setModeratePolicyViolationCount(MODERATE_POLICY_VIOLATION_COUNT);
    evaluationResult.setLegacyViolationCount(LEGACY_VIOLATION_COUNT);
    File resultFile = new File(tmpDir.getRoot(), "missing-dir/result.json");
    new ScanClient(config, "the-app-id").saveResultData(resultFile, receipt, evaluationResult, "Failure");
    ResultData data = JsonUtils.read(resultFile, ResultData.class);
    assertThat(data.scanId).isEqualTo(receipt.getScanId());
    assertThat(data.applicationId).isEqualTo("the-app-id");
    assertThat(data.reportHtmlUrl).isEqualTo(receipt.resolveReportUrl(config.getServerUrl()));
    assertThat(data.reportPdfUrl).isEqualTo(receipt.resolvePdfUrl(config.getServerUrl()));
    assertThat(data.reportDataUrl).isEqualTo(receipt.resolveDataUrl(config.getServerUrl()));
    assertThat(data.policyEvaluationResult.getCriticalComponentCount()).isEqualTo(CRITICAL_COMPONENT_COUNT);
    assertThat(data.policyEvaluationResult.getSevereComponentCount()).isEqualTo(SEVERE_COMPONENT_COUNT);
    assertThat(data.policyEvaluationResult.getModerateComponentCount()).isEqualTo(MODERATE_COMPONENT_COUNT);
    assertThat(data.policyEvaluationResult.getCriticalPolicyViolationCount())
        .isEqualTo(CRITICAL_POLICY_VIOLATION_COUNT);
    assertThat(data.policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(SEVERE_POLICY_VIOLATION_COUNT);
    assertThat(data.policyEvaluationResult.getModeratePolicyViolationCount())
        .isEqualTo(MODERATE_POLICY_VIOLATION_COUNT);
    assertThat(data.policyEvaluationResult.getLegacyViolationCount()).isEqualTo(LEGACY_VIOLATION_COUNT);
    assertThat(data.policyAction).isEqualTo("Failure");
  }

  @Test
  public void testUploadCLIScan() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ScanReceipt receipt = new ScanClient(config, APP_ID).uploadCLIScan(tmpDir.newFile("scan.xml.gz"),
        ClientScanType.SONATYPE);
    assertThat(receipt.getScanId()).isEqualTo("SCAN-ID");
    assertThat(receipt.getReportUrl()).isEqualTo("ui/links/application/ScanClientTest_AppId/report/SCAN-ID");
    assertThat(receipt.getPdfUrl()).isEqualTo("ui/links/application/ScanClientTest_AppId/report/SCAN-ID/pdf");
  }

  @Test
  public void testUploadCLIScan_InvalidAppId() {
    Configuration config = getCLMServer().getClientConfiguration();
    assertThatExceptionOfType(HttpResponseException.class)
        .isThrownBy(() -> new ScanClient(config, "invalid-id").uploadCLIScan(tmpDir.newFile("scan.xml.gz"),
            ClientScanType.SONATYPE))
        .withMessage("Could not find an application with public ID invalid-id.")
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  @Test
  public void testGetVulnerableComponentsWithSignatures() throws IOException {
    Configuration config = getCLMServer().getClientConfiguration();
    ScanClient scanClient = new ScanClient(config, APP_ID);

    String scanId = TemporaryEntity.uuid();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(applicationDAO.getByPublicId(APP_ID).getId(), BuildStageType.ID, scanId);
    mockReport(policyEvaluation, getClass().getSimpleName());

    Signature signature = new Signature();
    signature.setAnchor("test-anchor");
    signature
        .setFunctionSignature(new FunctionSignature("com/sonatype/insight/scan/cli/Main.main([Ljava/lang/String;)V"));

    ComponentWithSignatures component = new ComponentWithSignatures("pkg:maven/gid/aid@1.0?type=jar", signature);
    ComponentWithSignaturesList componentWithSignaturesList =
        new ComponentWithSignaturesList(Collections.singletonList(component));

    hdsRespondWith(componentWithSignaturesList).atUri("rest/component/signatures/vulnerability");

    ComponentWithSignaturesList result = scanClient.getVulnerableComponentsWithSignatures(scanId);
    assertThat(result)
        .isNotNull()
        .usingRecursiveComparison().isEqualTo(componentWithSignaturesList);
  }
}
