/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PolicyClientTest
    extends AbstractBrainServiceTest
{
  InsightWork insightWork;

  @Before
  public void setup() {
    insightWork = getCLMServer().getInstance(InsightWork.class);
  }

  @Test
  public void testLinkToManagement() {
    String appId = "app id";
    PolicyClient policyClient = new PolicyClient(getCLMServer().getClientConfiguration(), appId);
    UriBuilder uriBuilder = UriBuilder.fromPath(getCLMServer().getClientConfiguration().getServerUrl());
    uriBuilder.path(UserInterfaceLinksResource.RESOURCE_PATH).path(UserInterfaceLinksResource.MANAGEMENT_PATH);
    assertThat(policyClient.linkToManagement()).isEqualTo(uriBuilder.build(OwnerType.APPLICATION, appId).toString());
  }

  @Test
  public void testGetPolicyEvaluationSummary() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");

    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    PolicyClient policyClient = new PolicyClient(config, application.getId());

    PolicyEvaluationSummary policyEvaluationSummary = policyClient
        .getPolicyEvaluationSummary(new Stage(Stage.ID_BUILD));
    assertThat(policyEvaluationSummary).isNull();

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(),
        scanId);
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluationSummary = policyClient.getPolicyEvaluationSummary(new Stage(Stage.ID_BUILD));

    assertThat(policyEvaluationSummary).isNotNull();
    assertThat(policyEvaluationSummary.getReportUrl())
        .isEqualTo("ui/links/application/" + application.getPublicId() + "/report/" + scanId);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(1);
  }

  @Test
  public void testEvaluateCLI() throws Exception {
    assertEvaluationCLIwithThirdPartyScanContent(false);
  }

  @Test
  public void testEvaluateCLI_withThirdPartyScanContent() throws Exception {
    assertEvaluationCLIwithThirdPartyScanContent(true);
  }

  private void assertEvaluationCLIwithThirdPartyScanContent(boolean thirdPartyScanningEnabled) throws IOException {
    Stage stage = new Stage(Stage.ID_BUILD);
    final String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application, scanId);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport(scanId);

    PolicyEvaluationPollingResult completedResult = new PolicyEvaluationPollingResult();
    completedResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    completedResult.setResult(new PolicyEvaluationResult());

    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doReturn(completedResult).when(policyClient).parseResult(ArgumentMatchers.any(Result.class),
        eq(PolicyEvaluationPollingResult.class));
    ClientScanResult clientScanResult = new ClientScanResult(scanFile, thirdPartyScanningEnabled);
    PolicyEvaluationPollingResult policyEvaluationResult =
        policyClient.evaluateCLI(clientScanResult, ClientScanType.SONATYPE, stage);
    assertThat(policyEvaluationResult).isNotNull();
  }

  @Test
  public void testEvaluateCI() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    final String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application, scanId);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport(scanId);

    PolicyEvaluationPollingResult completedResult = new PolicyEvaluationPollingResult();
    completedResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    completedResult.setResult(new PolicyEvaluationResult());

    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doReturn(completedResult).when(policyClient)
        .parseResult(ArgumentMatchers.any(Result.class), eq(PolicyEvaluationPollingResult.class));
    ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);
    PolicyEvaluationPollingResult policyEvaluationResult = policyClient.evaluateCI(clientScanResult, stage);
    assertThat(policyEvaluationResult).isNotNull();
  }

  @Test
  public void testEvaluateRepoMan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    final String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application, scanId);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport(scanId);

    PolicyEvaluationPollingResult completedResult = new PolicyEvaluationPollingResult();
    completedResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    completedResult.setResult(new PolicyEvaluationResult());

    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doReturn(completedResult).when(policyClient)
        .parseResult(ArgumentMatchers.any(Result.class), eq(PolicyEvaluationPollingResult.class));

    PolicyEvaluationPollingResult policyEvaluationResult = policyClient.evaluateRepoMan(scanFile, stage);
    assertThat(policyEvaluationResult).isNotNull();
  }

  @Test
  public void testEvaluate_RetriesUntilComplete() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    final String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application, scanId);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport(scanId);

    PolicyEvaluationPollingResult pendingResult = new PolicyEvaluationPollingResult();
    pendingResult.setStatus(PolicyEvaluationStatus.PENDING);
    PolicyEvaluationPollingResult completedResult = new PolicyEvaluationPollingResult();
    completedResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    completedResult.setResult(new PolicyEvaluationResult());

    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doReturn(pendingResult).doReturn(pendingResult).doReturn(completedResult).when(policyClient)
        .parseResult(ArgumentMatchers.any(Result.class), eq(PolicyEvaluationPollingResult.class));

    PolicyEvaluationPollingResult policyEvaluationResult =
        policyClient.evaluate("cli", scanFile, ClientScanType.SONATYPE, stage);
    assertThat(policyEvaluationResult).isNotNull();
    verify(policyClient, times(3))
        .parseResult(ArgumentMatchers.any(Result.class), eq(PolicyEvaluationPollingResult.class));
  }

  @Test
  public void testEvaluate_DoesNotPollWhenPolicyEvaluationReceiptRequestFails() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    final String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application, scanId);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport(scanId);

    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doThrow(new IOException("EVALUATION REQUEST FAILURE")).when(policyClient)
        .parseResult(ArgumentMatchers.any(Result.class), eq(PolicyEvaluationReceipt.class));

    try {
      policyClient.evaluate("cli", scanFile, ClientScanType.SONATYPE, stage);
      fail("IOException expected to be thrown");
    }
    catch (IOException e) {
      assertThat(e.getMessage()).isEqualTo("EVALUATION REQUEST FAILURE");
      verify(policyClient, never())
          .parseResult(ArgumentMatchers.any(Result.class), eq(PolicyEvaluationPollingResult.class));
    }
  }

  @Test
  public void testEvaluate_StopsRetryingWhenFailed() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    final String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application, scanId);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport(scanId);

    PolicyEvaluationPollingResult failedResult = new PolicyEvaluationPollingResult();
    failedResult.setStatus(PolicyEvaluationStatus.FAILED);
    failedResult.setReason("FAILURE REASON");

    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doReturn(failedResult).when(policyClient).parseResult(ArgumentMatchers.any(Result.class),
        eq(PolicyEvaluationPollingResult.class));

    try {
      policyClient.evaluate("cli", scanFile, ClientScanType.SONATYPE, stage);
      fail("IOException expected to be thrown");
    }
    catch (IOException e) {
      assertThat(e.getMessage()).isEqualTo("Policy evaluation could not be completed: FAILURE REASON");
      verify(policyClient, times(1))
          .parseResult(ArgumentMatchers.any(Result.class), eq(PolicyEvaluationPollingResult.class));
    }
  }

  @Test
  public void testEvaluate_LogStatusAndScanId() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    final String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");

    File scanFile = createScanFile(application, scanId);
    ScanReceipt scanReceipt = mockScanReceiptAndReport(scanId);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));

    PolicyEvaluationReceipt receipt = new PolicyEvaluationReceipt();
    receipt.setStatusId("evaluation-statusid");
    Logger logger = mock(Logger.class);
    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId(), logger));
    doReturn(receipt).when(policyClient).parseResult(ArgumentMatchers.any(Result.class),
        eq(PolicyEvaluationReceipt.class));

    PolicyEvaluationPollingResult pollingResult = new PolicyEvaluationPollingResult();
    pollingResult.setScanReceipt(scanReceipt);
    pollingResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    doReturn(pollingResult).when(policyClient).parseResult(ArgumentMatchers.any(Result.class),
        eq(PolicyEvaluationPollingResult.class));

    policyClient.evaluate("cli", scanFile, ClientScanType.SONATYPE, stage);

    verify(logger).debug("Assigned status ID {}", "evaluation-statusid");
    verify(logger).info("Assigned scan ID {}", "test-scanid");
  }

  private File createScanFile(Application app, String scanId) {
    File scanFile = insightWork.getScanFile(app.getId(), scanId);

    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return scanFile;
  }

  private ScanReceipt mockScanReceiptAndReport(String scanId) {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    scanReceipt.setTimeToReport(1L);
    mockScanReceipt(scanReceipt);
    mockReport(scanId, "/PolicyClientTest/report.zip");

    return scanReceipt;
  }
}
