/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.service.githubapp.GitHubAppSelectionService;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultUtils;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanMetadata;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.sonatype.insight.brain.testsupport.TempFolder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.zeroturnaround.exec.InvalidExitValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SourceControlScanServiceTest
    extends VerifiableLoggingTestBase
{
  private static final String APP_ID = "app-id";

  @RegisterExtension
  public TempFolder tmpDir = new TempFolder();

  @Mock
  private GitApiFactory mockGitApiFactory;

  @Mock
  private GitClientFactory mockGitClientFactory;

  private SourceControlUtils spySourceControlUtils;

  @Mock
  private ApplicationDAO mockApplicationDAO;

  @Mock
  private GitRepositoryInfo mockGitRepositoryInfo;

  @Mock
  private GitApi mockGitApi;

  @Mock
  private PolicyEvaluateService policyEvaluateService;

  @Mock
  private PolicyEvaluationPollingResultUtils mockPolicyEvaluationPollingResultUtils;

  @Mock
  private ProprietaryConfigService proprietaryConfigService;

  @Mock
  private InsightWork mockInsightWork;

  @Mock
  private Scanner scanner;

  @Mock
  private SourceControlSshService sourceControlSshService;

  @Mock
  private AuditRecorder mockAuditRecorder;

  private Application application;

  private SourceControlEvent sourceControlEvent;

  private File sourceControlDir;

  private ScanResult scanResult;

  @Mock
  private FileCleaner fileCleaner;

  @Mock
  private GitHubAppSelectionService mockGitHubAppSelectionService;

  private ProprietaryConfig proprietaryConfig;

  private TestProductLicense testProductLicense;

  private IqForScmLicenseChecker licenseChecker;

  // subject
  private SourceControlScanService service;

  private Level originalLogLevel;

  public SourceControlScanServiceTest() {
    super(SourceControlScanService.class);
  }

  @BeforeEach
  @Override
  public void setup() {
    super.setup();

    try {
      sourceControlDir = tmpDir.newFolder();
    }
    catch (final IOException ioEx) {
      throw new RuntimeException("failed creating temp source control dir", ioEx);
    }

    application = new Application();
    application.setId(APP_ID);
    application.setPublicId("public-app-id");
    when(mockApplicationDAO.getByIdNotNull(eq(APP_ID))).thenReturn(application);

    when(mockInsightWork.getSourceControlDir(APP_ID)).thenReturn(new File(sourceControlDir, APP_ID));

    sourceControlEvent = new SourceControlEvent();
    sourceControlEvent.setApplicationId(APP_ID);

    spySourceControlUtils = spy(
        new SourceControlUtils(null, mockInsightWork, fileCleaner, mockGitClientFactory,
            mockGitHubAppSelectionService));

    TestProductLicenseManager productLicenseManager = new TestProductLicenseManager();
    testProductLicense = new TestProductLicense(productLicenseManager, mock(DeveloperEnablementService.class));
    testProductLicense.reset();

    licenseChecker = new IqForScmLicenseChecker(testProductLicense);

    service = new SourceControlScanService(mockGitApiFactory, spySourceControlUtils, mockApplicationDAO, licenseChecker,
        proprietaryConfigService, policyEvaluateService, mockPolicyEvaluationPollingResultUtils,
        scanner, mockAuditRecorder, sourceControlSshService);

    proprietaryConfig = new ProprietaryConfig();
    when(proprietaryConfigService.getProprietaryConfig(eq(OwnerType.APPLICATION), eq("public-app-id")))
        .thenReturn(proprietaryConfig);

    when(mockGitRepositoryInfo.getSourceControlEvaluationsEnabled()).thenReturn(true);

    // Set logger to DEBUG level to ensure all log messages are captured
    // This prevents flakiness when previous tests may have changed the logger level
    Logger log =
        (Logger) org.slf4j.LoggerFactory.getLogger(SourceControlScanService.class);
    originalLogLevel = log.getLevel();
    log.setLevel(Level.DEBUG);
  }

  @AfterEach
  public void teardown() {
    // Restore original logger level to avoid affecting other tests
    Logger log =
        (Logger) org.slf4j.LoggerFactory.getLogger(SourceControlScanService.class);
    log.setLevel(originalLogLevel);
  }

  @Test
  public void testOnSourceControlScan_WithNoSourceControl() throws Exception {
    // given there is no source control info for an application
    doReturn(null).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());

    // when we receive a source control scan event
    service.onSourceControlScan(sourceControlEvent);

    // then it hasn't create any new directories
    assertThat(sourceControlDir).isEmptyDirectory();

    // and it never tries any git operations
    verifyNoInteractions(mockGitApiFactory, mockGitApi);

    // and it never interacts with the SSH service
    verifyNoInteractions(sourceControlSshService);
  }

  @Test
  public void testOnSourceControlScan_triggerScan() throws Exception {
    // given an event
    sourceControlEvent.setBranchName("branch");
    sourceControlEvent.setStatusId("statusId");
    sourceControlEvent.setApplicationId(APP_ID);
    sourceControlEvent.setStageTypeId(Stage.ID_DEVELOP);
    sourceControlEvent.setUserAgent("userAgent");
    sourceControlEvent.setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // and a source control configuration
    doReturn(mockGitRepositoryInfo).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());
    when(mockGitApiFactory.createGitApi(mockGitRepositoryInfo)).thenReturn(mockGitApi);

    // and a scan result
    scanResult = new ScanResult();
    scanResult.setScanEntity(mock(ScanEntity.class));
    when(scanner.scan(any(List.class), eq(APP_ID), eq(proprietaryConfig), any(ScanConfiguration.class),
        any(ScanMetadata.class))).thenReturn(scanResult);

    // when we receive a source control scan event
    service.onSourceControlScan(sourceControlEvent);

    // then it creates the target directory
    assertThat(new File(sourceControlDir, APP_ID)).isDirectory();

    // and it calls the repository sync
    verify(mockGitApi).cloneOrPullRepository(isA(File.class), eq(sourceControlEvent.getBranchName()));

    // and it calls the scanner
    verify(scanner).scan(eq(Collections.singletonList(mockInsightWork.getSourceControlDir(APP_ID))), eq(APP_ID),
        eq(proprietaryConfig), any(ScanConfiguration.class), any(ScanMetadata.class));

    // and it evaluates a policy
    verify(policyEvaluateService).evaluateWithPolling(eq("statusId"),
        isA(Application.class), eq(ClientScanType.SONATYPE), argThat(s -> s.getStageTypeId().equals(Stage.ID_DEVELOP)),
        eq(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING), isA(ScanEntity.class), eq("api"),
        eq("userAgent"), any());

    verifySshServiceInvoked();
  }

  @Test
  public void testOnSourceControlScan_ExceptionBeforePolicyEvaluation() {
    // given an event
    String statusId = "testStatusId";
    sourceControlEvent.setBranchName("branch");
    sourceControlEvent.setStatusId(statusId);
    sourceControlEvent.setApplicationId(APP_ID);
    sourceControlEvent.setStageTypeId(Stage.ID_DEVELOP);
    sourceControlEvent.setUserAgent("userAgent");
    sourceControlEvent.setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_API);

    // and a source control configuration
    doReturn(mockGitRepositoryInfo).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());

    // and an exception is thrown when we process a source control scan event before the policy evaluation is called
    Exception testException = new RuntimeException("test exception");
    doThrow(testException).when(spySourceControlUtils)
        .getCheckoutDirectory(any(Application.class));
    assertThatThrownBy(() -> service.onSourceControlScan(sourceControlEvent))
        .isInstanceOf(RuntimeException.class)
        .hasMessage(testException.getMessage());

    // and there was no policy evaluation
    verify(policyEvaluateService, never()).evaluateWithPolling(any(), any(), any(), any(), any(), any(), any(), any(),
        any());

    verify(mockPolicyEvaluationPollingResultUtils).handleException(eq(APP_ID), eq(statusId), eq(testException));
  }

  @Test
  public void testOnSourceControlScan_sparseCheckoutEmpty() throws Exception {
    // given an event
    sourceControlEvent.setBranchName("branch");
    sourceControlEvent.setStatusId("statusId");
    sourceControlEvent.setApplicationId(APP_ID);
    sourceControlEvent.setStageTypeId(Stage.ID_DEVELOP);
    sourceControlEvent.setUserAgent("userAgent");
    sourceControlEvent.setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // and a source control configuration
    doReturn(mockGitRepositoryInfo).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());
    when(mockGitApiFactory.createGitApi(mockGitRepositoryInfo)).thenReturn(mockGitApi);

    // and a scan result
    scanResult = new ScanResult();
    scanResult.setScanEntity(mock(ScanEntity.class));
    when(scanner.scan(any(List.class), eq(APP_ID), eq(proprietaryConfig), any(ScanConfiguration.class),
        any(ScanMetadata.class))).thenReturn(scanResult);

    // Sparse checkout leaves no entry on working directory - exception thrown
    InvalidExitValueException innerException =
        new InvalidExitValueException("Sparse checkout leaves no entry on working directory", null);
    GitException exception = new GitException("Invalid exit code executing command", innerException);
    when(mockGitApi.cloneOrPullRepository(any(), any())).thenThrow(exception);

    // when we receive a source control scan event
    service.onSourceControlScan(sourceControlEvent);

    // then it creates the target directory
    assertThat(new File(sourceControlDir, APP_ID)).isDirectory();

    // and it calls the repository sync
    verify(mockGitApi).cloneOrPullRepository(isA(File.class), eq(sourceControlEvent.getBranchName()));

    // and it evaluates a policy
    verify(policyEvaluateService).evaluateWithPolling(eq("statusId"),
        isA(Application.class), eq(ClientScanType.SONATYPE), argThat(s -> s.getStageTypeId().equals(Stage.ID_DEVELOP)),
        eq(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING), isA(ScanEntity.class), eq("api"),
        eq("userAgent"), any());

    verifySshServiceInvoked();
  }

  @Test
  public void testDoSynchronousSourceControlScan() throws Exception {
    // and a source control configuration
    doReturn(mockGitRepositoryInfo).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());
    when(mockGitApiFactory.createGitApi(mockGitRepositoryInfo)).thenReturn(mockGitApi);

    // and a scan result
    scanResult = new ScanResult();
    scanResult.setScanEntity(mock(ScanEntity.class));
    ArgumentCaptor<ScanConfiguration> scanConfigurationArgCaptor = ArgumentCaptor.forClass(ScanConfiguration.class);
    when(scanner.scan(any(List.class), eq(APP_ID), eq(proprietaryConfig), any(ScanConfiguration.class),
        any(ScanMetadata.class))).thenReturn(scanResult);

    // and a policy evaluation
    Stage stage = new Stage(Stage.ID_DEVELOP);
    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    when(policyEvaluateService.evaluateSynchronousNoAuth(any(Application.class), any(ClientScanType.class),
        any(ScanEntity.class), eq(stage), eq(ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST), eq(null)))
            .thenReturn(policyEvaluation);

    // it evaluates the SCM repository content and it returns the expected policy evaluation
    assertThat(service.doSynchronousSourceControlScan(APP_ID, stage, "testBranchName")).isEqualTo(policyEvaluation);
    verify(scanner, times(1)).scan(any(), any(), any(), scanConfigurationArgCaptor.capture(), any());
    assertThat(scanConfigurationArgCaptor.getValue().getProperties().get("dirExcludes")).isEqualTo("**/src/test");

    verifySshServiceInvoked();
  }

  @Test
  public void testDoSynchronousSourceControlScan_forCommit() throws Exception {
    // given: a source control configuration
    doReturn(mockGitRepositoryInfo).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());
    when(mockGitApiFactory.createGitApi(mockGitRepositoryInfo)).thenReturn(mockGitApi);

    // and a scan result
    scanResult = new ScanResult();
    scanResult.setScanEntity(mock(ScanEntity.class));
    scanResult.setScanFile(mock(File.class));
    ArgumentCaptor<ScanConfiguration> scanConfigurationArgCaptor = ArgumentCaptor.forClass(ScanConfiguration.class);
    when(scanner.scan(any(List.class), eq(APP_ID), eq(proprietaryConfig), any(ScanConfiguration.class),
        any(ScanMetadata.class))).thenReturn(scanResult);

    // and a policy evaluation
    Stage stage = new Stage(Stage.ID_DEVELOP);
    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    when(policyEvaluateService.evaluateSynchronousNoAuth(any(Application.class), any(ClientScanType.class),
        any(ScanEntity.class), eq(stage), eq(ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST), eq(null)))
            .thenReturn(policyEvaluation);

    // when: we do synchronous source control scan and evaluate the SCM repository content
    PolicyEvaluation returnedPolicyEvaluation =
        service.doSynchronousSourceControlScan(APP_ID, stage, "testBranchName", "testCommitHash");

    // then: it returns the expected policy evaluation
    assertThat(returnedPolicyEvaluation).isEqualTo(policyEvaluation);
    verify(scanner, times(1)).scan(any(), any(), any(), scanConfigurationArgCaptor.capture(), any());
    assertThat(scanConfigurationArgCaptor.getValue().getProperties().get("dirExcludes")).isEqualTo("**/src/test");

    verifySshServiceInvoked();
  }

  @Test
  public void testDoSynchronousSourceControlScan_InternalSourceControlEvaluationsDisabled_null() throws Exception {
    testDoSynchronousSourceControlScan_InternalSourceControlEvaluationsDisabled(null);
  }

  @Test
  public void testDoSynchronousSourceControlScan_InternalSourceControlEvaluationsDisabled_false() throws Exception {
    testDoSynchronousSourceControlScan_InternalSourceControlEvaluationsDisabled(false);
  }

  private void testDoSynchronousSourceControlScan_InternalSourceControlEvaluationsDisabled(
      Boolean internalSourceControlPolicyEvaluationsEnabled) throws Exception
  {
    // given internal SCM policy evaluations are disabled
    doReturn(mockGitRepositoryInfo).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());
    when(mockGitRepositoryInfo.getSourceControlEvaluationsEnabled())
        .thenReturn(internalSourceControlPolicyEvaluationsEnabled);

    // it does not evaluate the SCM repository content and it returns null
    assertThat(service.doSynchronousSourceControlScan(APP_ID, new Stage(Stage.ID_DEVELOP), "testBranchName")).isNull();

    // and it never interacts with the SSH service
    verifyNoInteractions(sourceControlSshService);
  }

  @Test
  public void testDoSynchronousSourceControlScan_Unlicensed() throws Exception {
    // given a product license without the automation and notifications features
    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION, LicensedFeature.NOTIFICATIONS);

    // it does not evaluate the SCM repository content and it returns null
    assertThat(service.doSynchronousSourceControlScan(APP_ID, new Stage(Stage.ID_DEVELOP), "testBranchName")).isNull();
    assertThatLogMessagesEqual(debug("License does not support source control notification or automation features"));

    // and it never interacts with the SSH service
    verifyNoInteractions(sourceControlSshService);
  }

  @Test
  public void testOnSourceControlScan_WithScanTargets() throws Exception {
    // given an event
    List<String> scanTargets = Arrays.asList("testScanTarget1", "testScanTarget2");
    sourceControlEvent.setBranchName("branch");
    sourceControlEvent.setScanTargets(scanTargets);
    sourceControlEvent.setStatusId("statusId");
    sourceControlEvent.setApplicationId(APP_ID);
    sourceControlEvent.setStageTypeId(Stage.ID_DEVELOP);
    sourceControlEvent.setUserAgent("userAgent");
    sourceControlEvent.setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_API);

    // and a source control configuration
    doReturn(mockGitRepositoryInfo).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());
    when(mockGitApiFactory.createGitApi(mockGitRepositoryInfo)).thenReturn(mockGitApi);

    // and a scan result
    scanResult = new ScanResult();
    scanResult.setScanEntity(mock(ScanEntity.class));
    when(scanner.scan(any(List.class), eq(APP_ID), eq(proprietaryConfig), any(ScanConfiguration.class),
        any(ScanMetadata.class))).thenReturn(scanResult);

    // when we receive a source control scan event
    service.onSourceControlScan(sourceControlEvent);

    // then it creates the target directory
    assertThat(new File(sourceControlDir, APP_ID)).isDirectory();

    // and it calls the repository sync
    verify(mockGitApi).cloneOrPullRepository(isA(File.class), eq(sourceControlEvent.getBranchName()));

    // and it calls the scanner with the specified scan targets
    File sourceControlDir = mockInsightWork.getSourceControlDir(APP_ID);
    List<File> expectedScanTargets =
        scanTargets.stream().map(scanTarget -> new File(sourceControlDir, scanTarget)).collect(Collectors.toList());
    verify(scanner).scan(eq(expectedScanTargets), eq(APP_ID), eq(proprietaryConfig), any(ScanConfiguration.class),
        any(ScanMetadata.class));

    // and it evaluates a policy
    verify(policyEvaluateService).evaluateWithPolling(eq("statusId"), isA(Application.class),
        eq(ClientScanType.SONATYPE), argThat(s -> s.getStageTypeId().equals(Stage.ID_DEVELOP)),
        eq(ScanTriggerType.SOURCE_CONTROL_API), isA(ScanEntity.class), eq("api"), eq("userAgent"), any());

    verifySshServiceInvoked();
  }

  private void verifySshServiceInvoked() {
    verify(sourceControlSshService, times(1)).verifySshUrlAndUpdateIfNeeded(APP_ID);
  }

  // CLM-34834: SSH URL recovery must run before createGitApi, and createGitApi must receive the
  // refreshed GitRepositoryInfo (with the freshly persisted SSH URL) rather than the stale one.
  @Test
  public void testOnSourceControlScan_sshReEnabled_populatesSshUrlBeforeCreatingGitApi() throws Exception {
    sourceControlEvent.setBranchName("branch");
    sourceControlEvent.setStatusId("statusId");
    sourceControlEvent.setApplicationId(APP_ID);
    sourceControlEvent.setStageTypeId(Stage.ID_DEVELOP);
    sourceControlEvent.setUserAgent("userAgent");
    sourceControlEvent.setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_API);

    GitRepositoryInfo staleGitRepositoryInfo = mock(GitRepositoryInfo.class);
    GitRepositoryInfo refreshedGitRepositoryInfo = mock(GitRepositoryInfo.class);

    // first call returns the stale info (SSH URL empty), second call after the SSH recovery returns the refreshed info.
    doReturn(staleGitRepositoryInfo, refreshedGitRepositoryInfo).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(APP_ID);
    when(mockGitApiFactory.createGitApi(refreshedGitRepositoryInfo)).thenReturn(mockGitApi);

    scanResult = new ScanResult();
    scanResult.setScanEntity(mock(ScanEntity.class));
    when(scanner.scan(any(List.class), eq(APP_ID), eq(proprietaryConfig), any(ScanConfiguration.class),
        any(ScanMetadata.class))).thenReturn(scanResult);

    service.onSourceControlScan(sourceControlEvent);

    InOrder inOrder = inOrder(sourceControlSshService, mockGitApiFactory);
    inOrder.verify(sourceControlSshService).verifySshUrlAndUpdateIfNeeded(APP_ID);
    inOrder.verify(mockGitApiFactory).createGitApi(refreshedGitRepositoryInfo);
    verify(mockGitApiFactory, never()).createGitApi(staleGitRepositoryInfo);
  }
}
