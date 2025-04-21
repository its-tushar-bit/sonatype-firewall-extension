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
import java.util.ConcurrentModificationException;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.openjpa.persistence.RollbackException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.insight.brain.utils.VulnerabilitySignatureAnalysisDTOHelper.createTestAnalysisDTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PolicyClientTest
    extends AbstractBrainServiceIntegrationTest
{
  private static final String SCAN_ID = "test-scanid";
  
  InsightWork insightWork;

  @Before
  public void setup() {
    insightWork = getCLMServer().getInstance(InsightWork.class);
  }

  @After
  public void after() throws InterruptedException {
    // We need to do this special cleanup because these tests start async policy evaluations but they don't wait for the
    // policy evaluations to finish.
    // This means the tests usually finish before the policy evaluations finish, which creates a race condition with the
    // TemporaryEntity cleanup.
    long start = System.currentTimeMillis();
    while (true) {
      try {
        tempEntity.after();
        return;
      }
      catch (RollbackException | ConcurrentModificationException e) {
        // 10 secs is usually enough time for the async policy evaluations to finish.
        if (System.currentTimeMillis() - start > 10000) {
          throw e;
        }
        Thread.sleep(50);
      }
    }
  }

  @Test
  public void testLinkToManagement() {
    String appId = "app id";
    PolicyClient policyClient = new PolicyClient(getCLMServer().getClientConfiguration(), appId);
    UriBuilder uriBuilder = UriBuilder.fromPath(getCLMServer().getClientConfiguration().getServerUrl());
    uriBuilder.path(UserInterfaceLinksHelper.RESOURCE_PATH).path(UserInterfaceLinksHelper.MANAGEMENT_PATH);
    assertThat(policyClient.linkToManagement()).isEqualTo(uriBuilder.build(OwnerType.APPLICATION, appId).toString());
  }

  @Test
  public void testGetPolicyEvaluationSummary() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");

    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    PolicyClient policyClient = new PolicyClient(config, application.getId());

    PolicyEvaluationSummary policyEvaluationSummary = policyClient
        .getPolicyEvaluationSummary(new Stage(Stage.ID_BUILD));
    assertThat(policyEvaluationSummary).isNull();

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(),
        SCAN_ID);
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluationSummary = policyClient.getPolicyEvaluationSummary(new Stage(Stage.ID_BUILD));

    assertThat(policyEvaluationSummary).isNotNull();
    assertThat(policyEvaluationSummary.getReportUrl())
        .isEqualTo("ui/links/application/" + application.getPublicId() + "/report/" + SCAN_ID);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(1);
  }

  @Test
  public void testEvaluateCLI() throws Exception {
    assertEvaluationCLIWithThirdPartyScanContent(false);
  }

  @Test
  public void testEvaluateCLI_withThirdPartyScanContent() throws Exception {
    assertEvaluationCLIWithThirdPartyScanContent(true);
  }

  private void assertEvaluationCLIWithThirdPartyScanContent(boolean thirdPartyScanningEnabled) throws IOException {
    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport();

    PolicyEvaluationPollingResult completedResult = new PolicyEvaluationPollingResult();
    completedResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    completedResult.setResult(new PolicyEvaluationResult());

    Logger logger = mock(Logger.class);
    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId(), logger));
    doReturn(completedResult).when(policyClient).parseResult(any(Result.class),
        eq(PolicyEvaluationPollingResult.class));
    ClientScanResult clientScanResult = new ClientScanResult(scanFile, thirdPartyScanningEnabled);
    PolicyEvaluationPollingResult policyEvaluationResult =
        policyClient.evaluateCLI(clientScanResult, ClientScanType.SONATYPE, stage);
    assertThat(policyEvaluationResult).isNotNull();

    verify(logger).debug("Amending source control record for application with id: {} with discovered repository URL",
        "test-app");
    verify(logger, never()).debug("Repository URL for application with id: {} could not be found.", "test-app");
  }

  @Test
  public void testContinueEvaluateCLI() throws Exception {
    assertContinueEvaluationCLIWithThirdPartyScanContent(false);
  }

  @Test
  public void testContinueEvaluateCLI_withThirdPartyScanContent() throws Exception {
    assertContinueEvaluationCLIWithThirdPartyScanContent(true);
  }

  private void assertContinueEvaluationCLIWithThirdPartyScanContent(boolean thirdPartyScanningEnabled)
      throws Exception
  {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);

    Application application = tempEntity.newApplicationWithParent();
    File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    Configuration config = getCLMServer().getClientConfiguration();
    Logger logger = mock(Logger.class);
    PolicyClient policyClient = new PolicyClient(config, application.getPublicId(), logger);

    ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);
    Stage stage = new Stage(Stage.ID_BUILD);
    PolicyEvaluationPollingResult componentAnalyzePollingResult =
        policyClient.runComponentAnalysisForCLI(clientScanResult, ClientScanType.SONATYPE, stage);

    ComponentIdentifier componentIdentifier = createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        componentAnalyzePollingResult.getScanReceipt().getScanId(),
        componentIdentifier,
        vulnerabilityIdentifier,
        lookup(InsightWork.class)
    );

    PolicyEvaluationPollingResult policyEvaluationResult = policyClient.runPolicyEvaluationForCLI(
        new ClientScanResult(scanFile, thirdPartyScanningEnabled),
        ClientScanType.SONATYPE,
        stage,
        componentAnalyzePollingResult.getStatusId(),
        analysisDTO
    );
    assertThat(policyEvaluationResult).isNotNull();

    verify(logger, times(2))
        .debug("Amending source control record for application with id: {} with discovered repository URL",
            application.getPublicId()
        );
    verify(logger, never())
        .debug("Repository URL for application with id: {} could not be found.", application.getPublicId());
  }

  @Test
  public void testContinueEvaluateCLI_RetriesUntilComplete() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);

    Application application = tempEntity.newApplicationWithParent();
    File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    Configuration config = getCLMServer().getClientConfiguration();
    Logger logger = mock(Logger.class);
    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId(), logger));

    ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);
    Stage stage = new Stage(Stage.ID_BUILD);
    PolicyEvaluationPollingResult componentAnalyzePollingResult =
        policyClient.runComponentAnalysisForCLI(clientScanResult, ClientScanType.SONATYPE, stage);

    ComponentIdentifier componentIdentifier = createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        componentAnalyzePollingResult.getScanReceipt().getScanId(),
        componentIdentifier,
        vulnerabilityIdentifier,
        lookup(InsightWork.class)
    );

    PolicyEvaluationPollingResult pendingResult = new PolicyEvaluationPollingResult();
    pendingResult.setStatus(PolicyEvaluationStatus.PENDING);
    PolicyEvaluationPollingResult completedResult = new PolicyEvaluationPollingResult();
    completedResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    completedResult.setResult(new PolicyEvaluationResult());

    doReturn(pendingResult)
        .doReturn(pendingResult)
        .doReturn(completedResult)
        .when(policyClient)
        .parseResult(any(Result.class), eq(PolicyEvaluationPollingResult.class));

    PolicyEvaluationPollingResult policyEvaluationResult = policyClient.runPolicyEvaluationForCLI(
        new ClientScanResult(scanFile, false),
        ClientScanType.SONATYPE,
        stage,
        componentAnalyzePollingResult.getStatusId(),
        analysisDTO
    );

    assertThat(policyEvaluationResult).isNotNull();

    verify(policyClient, times(5))
        .parseResult(any(Result.class), eq(PolicyEvaluationPollingResult.class));
  }

  @Test
  public void testContinueEvaluateCLI_DoesNotPollWhenPolicyEvaluationReceiptRequestFails() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);

    Application application = tempEntity.newApplicationWithParent();
    File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    Configuration config = getCLMServer().getClientConfiguration();
    Logger logger = mock(Logger.class);
    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId(), logger));

    ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);
    Stage stage = new Stage(Stage.ID_BUILD);
    PolicyEvaluationPollingResult componentAnalyzePollingResult =
        policyClient.runComponentAnalysisForCLI(clientScanResult, ClientScanType.SONATYPE, stage);

    ComponentIdentifier componentIdentifier = createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        componentAnalyzePollingResult.getScanReceipt().getScanId(),
        componentIdentifier,
        vulnerabilityIdentifier,
        lookup(InsightWork.class)
    );

    doThrow(IOException.class).when(policyClient)
        .parseResult(any(Result.class), eq(PolicyEvaluationReceipt.class));

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> policyClient.runPolicyEvaluationForCLI(
            new ClientScanResult(scanFile, false),
            ClientScanType.SONATYPE,
            stage,
            componentAnalyzePollingResult.getStatusId(),
            analysisDTO
        ));

    verify(policyClient, times(2))
        .parseResult(any(Result.class), eq(PolicyEvaluationPollingResult.class));
  }

  @Test
  public void testContinueEvaluateCLI_StopsRetryingWhenFailed() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);

    Application application = tempEntity.newApplicationWithParent();
    File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    Configuration config = getCLMServer().getClientConfiguration();
    Logger logger = mock(Logger.class);
    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId(), logger));

    ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);
    Stage stage = new Stage(Stage.ID_BUILD);
    PolicyEvaluationPollingResult componentAnalyzePollingResult =
        policyClient.runComponentAnalysisForCLI(clientScanResult, ClientScanType.SONATYPE, stage);

    ComponentIdentifier componentIdentifier = createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        componentAnalyzePollingResult.getScanReceipt().getScanId(),
        componentIdentifier,
        vulnerabilityIdentifier,
        lookup(InsightWork.class)
    );

    String failureReason = "FAILURE REASON";
    PolicyEvaluationPollingResult failedResult = new PolicyEvaluationPollingResult();
    failedResult.setStatus(PolicyEvaluationStatus.FAILED);
    failedResult.setReason(failureReason);

    doReturn(failedResult)
        .when(policyClient)
        .parseResult(any(Result.class), eq(PolicyEvaluationPollingResult.class));

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() ->
            policyClient.runPolicyEvaluationForCLI(
            new ClientScanResult(scanFile, false),
            ClientScanType.SONATYPE,
            stage,
            componentAnalyzePollingResult.getStatusId(),
            analysisDTO
        )).withMessage("Policy evaluation could not be completed: " + failureReason);

    verify(policyClient, times(3))
        .parseResult(any(Result.class), eq(PolicyEvaluationPollingResult.class));
  }

  @Test
  public void testEvaluateCI() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport();

    Logger logger = mock(Logger.class);
    PolicyClient policyClient = new PolicyClient(config, application.getPublicId(), logger);
    ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);
    PolicyEvaluationPollingResult policyEvaluationResult = policyClient.evaluateCI(clientScanResult, stage);
    assertThat(policyEvaluationResult).isNotNull();

    verify(logger, never()).debug(
        "Amending source control record for application with id: {} with discovered repository URL", "test-app");
    verify(logger, never()).debug("Repository URL for application with id: {} could not be found.", "test-app");
  }

  @Test
  public void testContinueEvaluateCI() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);

    Application application = tempEntity.newApplicationWithParent();
    File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    Configuration config = getCLMServer().getClientConfiguration();
    Logger logger = mock(Logger.class);
    PolicyClient policyClient = new PolicyClient(config, application.getPublicId(), logger);

    ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);
    Stage stage = new Stage(Stage.ID_BUILD);
    PolicyEvaluationPollingResult componentAnalyzePollingResult =
        policyClient.runComponentAnalysisForCI(clientScanResult, stage);

    ComponentIdentifier componentIdentifier = createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        componentAnalyzePollingResult.getScanReceipt().getScanId(),
        componentIdentifier,
        vulnerabilityIdentifier,
        lookup(InsightWork.class)
    );

    PolicyEvaluationPollingResult policyEvaluationResult = policyClient.runPolicyEvaluationForCI(
        clientScanResult,
        stage,
        componentAnalyzePollingResult.getStatusId(),
        analysisDTO
    );
    assertThat(policyEvaluationResult).isNotNull();

    verify(logger, never()).debug(
        "Amending source control record for application with id: {} with discovered repository URL",
        application.getPublicId()
    );
    verify(logger, never())
        .debug("Repository URL for application with id: {} could not be found.", application.getPublicId());
  }

  @Test
  public void testEvaluateRepoMan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport();

    PolicyClient policyClient = new PolicyClient(config, application.getPublicId());

    PolicyEvaluationPollingResult policyEvaluationResult = policyClient.evaluateRepoMan(scanFile, stage);
    assertThat(policyEvaluationResult).isNotNull();
  }

  @Test
  public void testEvaluate_RetriesUntilComplete() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport();

    PolicyEvaluationPollingResult pendingResult = new PolicyEvaluationPollingResult();
    pendingResult.setStatus(PolicyEvaluationStatus.PENDING);
    PolicyEvaluationPollingResult completedResult = new PolicyEvaluationPollingResult();
    completedResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    completedResult.setResult(new PolicyEvaluationResult());

    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doReturn(pendingResult).doReturn(pendingResult).doReturn(completedResult).when(policyClient)
        .parseResult(any(Result.class), eq(PolicyEvaluationPollingResult.class));

    PolicyEvaluationPollingResult policyEvaluationResult =
        policyClient.evaluate("cli", scanFile, ClientScanType.SONATYPE, stage);
    assertThat(policyEvaluationResult).isNotNull();
    verify(policyClient, times(3))
        .parseResult(any(Result.class), eq(PolicyEvaluationPollingResult.class));
  }

  @Test
  public void testEvaluate_DoesNotPollWhenPolicyEvaluationReceiptRequestFails() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport();

    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doThrow(new IOException("EVALUATION REQUEST FAILURE")).when(policyClient)
        .parseResult(any(Result.class), eq(PolicyEvaluationReceipt.class));

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> policyClient.evaluate("cli", scanFile, ClientScanType.SONATYPE, stage))
        .withMessage("EVALUATION REQUEST FAILURE");
    verify(policyClient, never()).parseResult(any(Result.class),
        eq(PolicyEvaluationPollingResult.class));
  }

  @Test
  public void testEvaluate_StopsRetryingWhenFailed() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    File scanFile = createScanFile(application);
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    mockScanReceiptAndReport();

    PolicyEvaluationPollingResult failedResult = new PolicyEvaluationPollingResult();
    failedResult.setStatus(PolicyEvaluationStatus.FAILED);
    failedResult.setReason("FAILURE REASON");

    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doReturn(failedResult).when(policyClient).parseResult(any(Result.class),
        eq(PolicyEvaluationPollingResult.class));

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> policyClient.evaluate("cli", scanFile, ClientScanType.SONATYPE, stage))
        .withMessage("Policy evaluation could not be completed: FAILURE REASON");
    verify(policyClient, times(1)).parseResult(any(Result.class),
        eq(PolicyEvaluationPollingResult.class));
  }

  @Test
  public void testEvaluate_LogStatusAndScanId() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");

    File scanFile = createScanFile(application);
    ScanReceipt scanReceipt = mockScanReceiptAndReport();
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));

    PolicyEvaluationReceipt receipt = new PolicyEvaluationReceipt();
    receipt.setStatusId("evaluation-statusid");
    Logger logger = mock(Logger.class);
    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId(), logger));
    doReturn(receipt).when(policyClient).parseResult(any(Result.class),
        eq(PolicyEvaluationReceipt.class));

    PolicyEvaluationPollingResult pollingResult = new PolicyEvaluationPollingResult();
    pollingResult.setScanReceipt(scanReceipt);
    pollingResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    doReturn(pollingResult).when(policyClient).parseResult(any(Result.class),
        eq(PolicyEvaluationPollingResult.class));

    policyClient.evaluate("cli", scanFile, ClientScanType.SONATYPE, stage);

    verify(logger).debug("Assigned status ID {} for evaluation", "evaluation-statusid");
    verify(logger).info("Assigned scan ID {} for evaluation", SCAN_ID);
  }

  @Test
  public void testImportReachabilityAnalysis() throws IOException {
    Application application = tempEntity.newApplicationWithParent("test-app");
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));

    VulnerabilitySignatureAnalysisDTO analysisDTO = new VulnerabilitySignatureAnalysisDTO();

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> policyClient.importReachabilityAnalysis(SCAN_ID, analysisDTO))
        .withMessage("No vulnerability signatures specified");

    verify(policyClient, times(1)).parseResult(any(Result.class), eq(PolicyEvaluationResult.class));
  }

  @Test
  public void testRunComponentAnalysisForCI() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    final Application application = tempEntity.newApplicationWithParent();
    final File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    final Configuration config = getCLMServer().getClientConfiguration();
    final Logger logger = mock(Logger.class);
    final PolicyClient policyClient = new PolicyClient(config, application.getPublicId(), logger);

    final ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);
    final Stage stage = new Stage(Stage.ID_BUILD);
    final PolicyEvaluationPollingResult policyEvaluationResult =
        policyClient.runComponentAnalysisForCI(clientScanResult, stage);

    assertThat(policyEvaluationResult).isNotNull();
    assertThat(policyEvaluationResult.getStatus()).isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(policyEvaluationResult.getSubStatus()).isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);

    // addOrUpdateSourceControl is not called for CI
    verify(logger, never())
        .debug("Amending source control record for application with id: {} with discovered repository URL",
            application.getPublicId());
    verify(logger, never())
        .debug("Repository URL for application with id: {} could not be found.", application.getPublicId());

    verify(logger).debug(eq("Assigned status ID {} for component analysis"), anyString());
    verify(logger).info("Assigned scan ID {} for component analysis", SCAN_ID);
    verify(logger).info(eq("Component analysis completed in {} seconds."), anyLong());
  }

  @Test
  public void testRunComponentAnalysisForCLI() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    final Application application = tempEntity.newApplicationWithParent();
    final File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    final Configuration config = getCLMServer().getClientConfiguration();
    final Logger logger = mock(Logger.class);
    final PolicyClient policyClient = new PolicyClient(config, application.getPublicId(), logger);

    final ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);
    final Stage stage = new Stage(Stage.ID_BUILD);
    final PolicyEvaluationPollingResult policyEvaluationResult =
        policyClient.runComponentAnalysisForCLI(clientScanResult, ClientScanType.SONATYPE, stage);

    assertThat(policyEvaluationResult).isNotNull();
    assertThat(policyEvaluationResult.getStatus()).isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(policyEvaluationResult.getSubStatus()).isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);

    // addOrUpdateSourceControl is called for CLI
    verify(logger)
        .debug("Amending source control record for application with id: {} with discovered repository URL",
            application.getPublicId());
    verify(logger, never())
        .debug("Repository URL for application with id: {} could not be found.", application.getPublicId());

    verify(logger).debug(eq("Assigned status ID {} for component analysis"), anyString());
    verify(logger).info("Assigned scan ID {} for component analysis", SCAN_ID);
    verify(logger).info(eq("Component analysis completed in {} seconds."), anyLong());
  }

  @Test
  public void testRunComponentAnalysis_RetriesUntilComplete() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    final Application application = tempEntity.newApplicationWithParent("test-app");
    final File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    final Configuration config = getCLMServer().getClientConfiguration();
    final PolicyEvaluationPollingResult pendingResult = new PolicyEvaluationPollingResult();
    pendingResult.setStatus(PolicyEvaluationStatus.PENDING);
    pendingResult.setSubStatus(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);
    final PolicyEvaluationPollingResult completedResult = new PolicyEvaluationPollingResult();
    completedResult.setStatus(PolicyEvaluationStatus.PENDING);
    completedResult.setSubStatus(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);
    completedResult.setResult(new PolicyEvaluationResult());

    final PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doReturn(pendingResult).doReturn(pendingResult).doReturn(completedResult).when(policyClient)
        .parseResult(any(Result.class), eq(PolicyEvaluationPollingResult.class));

    final PolicyEvaluationPollingResult policyEvaluationResult =
        policyClient.runComponentAnalysis("cli", scanFile, ClientScanType.SONATYPE, new Stage(Stage.ID_BUILD));

    assertThat(policyEvaluationResult).isNotNull();

    verify(policyClient, times(3))
        .parseResult(any(Result.class), eq(PolicyEvaluationPollingResult.class));
  }

  @Test
  public void testRunComponentAnalysis_DoesNotPollWhenPolicyEvaluationReceiptRequestFails() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    final Application application = tempEntity.newApplicationWithParent();
    final File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    final Configuration config = getCLMServer().getClientConfiguration();
    final PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));
    doThrow(IOException.class).when(policyClient)
        .parseResult(any(Result.class), eq(PolicyEvaluationReceipt.class));

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> policyClient
            .runComponentAnalysis("cli", scanFile, ClientScanType.SONATYPE, new Stage(Stage.ID_BUILD)));

    verify(policyClient, never()).parseResult(any(Result.class),
        eq(PolicyEvaluationPollingResult.class));
  }

  @Test
  public void testRunComponentAnalysis_StopsRetryingWhenFailed() throws Exception {
    SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.setEnabled(true);
    final Application application = tempEntity.newApplicationWithParent();
    final File scanFile = createScanFile(application);
    mockScanReceiptAndReport();

    final Configuration config = getCLMServer().getClientConfiguration();
    PolicyClient policyClient = spy(new PolicyClient(config, application.getPublicId()));

    final String failureReason = "FAILURE REASON";
    PolicyEvaluationPollingResult failedResult = new PolicyEvaluationPollingResult();
    failedResult.setStatus(PolicyEvaluationStatus.FAILED);
    failedResult.setSubStatus(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);
    failedResult.setReason(failureReason);
    doReturn(failedResult).when(policyClient).parseResult(any(Result.class),
        eq(PolicyEvaluationPollingResult.class));

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> policyClient
            .runComponentAnalysis("cli", scanFile, ClientScanType.SONATYPE, new Stage(Stage.ID_BUILD)))
        .withMessage("Component analysis could not be completed: " + failureReason);

    verify(policyClient, times(1)).parseResult(any(Result.class),
        eq(PolicyEvaluationPollingResult.class));
  }

  private File createScanFile(Application app) {
    File scanFile = insightWork.getScanFile(app.getId(), SCAN_ID);

    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return scanFile;
  }

  private ScanReceipt mockScanReceiptAndReport() {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(SCAN_ID);
    scanReceipt.setTimeToReport(1L);
    mockScanReceipt(scanReceipt);
    mockReport(SCAN_ID, "/PolicyClientTest/report.zip");

    return scanReceipt;
  }
}
