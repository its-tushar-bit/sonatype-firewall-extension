/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.SourceControlConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ScanMetadata;
import com.sonatype.nexus.git.utils.api.GitApi;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SourceControlScanServiceTest
    extends VerifiableLoggingTestBase
{
  private static final String APP_ID = "app-id";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Mock
  private GitApiFactory mockGitApiFactory;

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
  private ProprietaryConfigService proprietaryConfigService;

  @Mock
  private InsightWork mockInsightWork;

  @Mock
  private Scanner scanner;

  @Mock
  private AuditRecorder mockAuditRecorder;

  private SourceControlConfig sourceControlConfig;

  private Application application;

  private SourceControlEvent sourceControlEvent;

  private File sourceControlDir;

  private ScanResult scanResult;

  @Mock
  private FileCleaner fileCleaner;

  private ProprietaryConfig proprietaryConfig;

  // subject
  private SourceControlScanService service;

  private final InsightConfig insightConfig = new InsightConfig();

  public SourceControlScanServiceTest() {
    super(SourceControlScanService.class);
  }

  @Before
  @Override
  public void setup() {
    super.setup();

    try {
      sourceControlDir = tmpDir.newFolder();
    }
    catch (final IOException ioEx) {
      throw new RuntimeException("failed creating temp source control dir", ioEx);
    }

    sourceControlConfig = new SourceControlConfig();
    sourceControlConfig.setCloneDirectory(sourceControlDir.getAbsolutePath());

    application = new Application();
    application.setId(APP_ID);
    application.setPublicId("public-app-id");
    when(mockApplicationDAO.getByIdNotNull(eq(APP_ID))).thenReturn(application);

    when(mockInsightWork.getSourceControlDir(APP_ID)).thenReturn(new File(sourceControlDir, APP_ID));

    sourceControlEvent = new SourceControlEvent();
    sourceControlEvent.setApplicationId(APP_ID);

    spySourceControlUtils = spy(new SourceControlUtils(null, mockApplicationDAO, mockInsightWork, fileCleaner));

    service = new SourceControlScanService(mockGitApiFactory, spySourceControlUtils, mockApplicationDAO,
        proprietaryConfigService, policyEvaluateService, mockInsightWork, scanner, mockAuditRecorder, insightConfig);

    proprietaryConfig = new ProprietaryConfig();
    when(proprietaryConfigService.getProprietaryConfig(eq(OwnerType.APPLICATION), eq("public-app-id")))
        .thenReturn(proprietaryConfig);
  }

  @Test
  public void testOnManifestScan_WithNoSourceControl() throws Exception {
    // given there is no source control info for an application
    doReturn(null).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());

    // when we receive a source control scan event
    service.onSourceControlScan(sourceControlEvent);

    // then it hasn't create any new directories
    assertThat(sourceControlDir).isEmptyDirectory();

    // and it never tries any git operations
    verifyNoInteractions(mockGitApiFactory, mockGitApi);
  }

  @Test
  public void testOnManifestScan_triggerScan() throws Exception {
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
    File scanDir = mock(File.class);
    scanResult.setScanFile(mock(File.class));
    when(mockInsightWork.getScanDir(eq(APP_ID))).thenReturn(scanDir);
    when(scanner.scan(any(File.class), isNull(), eq(scanDir), eq(proprietaryConfig), any(ScanMetadata.class)))
        .thenReturn(scanResult);

    // when we receive a source control scan event
    service.onSourceControlScan(sourceControlEvent);

    // then it creates the target directory
    assertThat(new File(sourceControlDir, APP_ID)).isDirectory();

    // and it calls the repository sync
    verify(mockGitApi).cloneOrPullRepository(isA(File.class), eq(sourceControlEvent.getBranchName()));

    // and it evaluates a policy
    verify(policyEvaluateService).evaluateWithPolling(eq("statusId"),
        isA(Application.class), eq(ClientScanType.SONATYPE), argThat(s -> s.getStageTypeId().equals(Stage.ID_DEVELOP)),
        eq(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING), isA(File.class), eq("api"),
        eq("userAgent"));
  }

  @Test
  public void testDoSynchronousSourceControlScan() throws Exception {
    // and a source control configuration
    doReturn(mockGitRepositoryInfo).when(spySourceControlUtils)
        .getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId());
    when(mockGitApiFactory.createGitApi(mockGitRepositoryInfo)).thenReturn(mockGitApi);

    // and a scan result
    scanResult = new ScanResult();
    File scanDir = mock(File.class);
    scanResult.setScanFile(mock(File.class));
    when(mockInsightWork.getScanDir(eq(APP_ID))).thenReturn(scanDir);
    when(scanner.scan(any(File.class), isNull(), eq(scanDir), eq(proprietaryConfig), any(ScanMetadata.class)))
        .thenReturn(scanResult);

    // and a policy evaluation
    Stage stage = new Stage(Stage.ID_DEVELOP);
    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    when(policyEvaluateService.evaluateSynchronousNoAuth(any(Application.class), any(ClientScanType.class),
        any(File.class), eq(stage), eq(ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST), eq(null)))
            .thenReturn(policyEvaluation);

    // it evaluates the SCM repository content and it returns the expected policy evaluation
    assertThat(service.doSynchronousSourceControlScan(APP_ID, stage, "testBranchName")).isEqualTo(policyEvaluation);
  }

  @Test
  public void testDoSynchronousSourceControlScan_InternalSourceControlPolicyEvaluationsDisabled() throws Exception {
    // given internal SCM policy evaluations are disable
    insightConfig.setFeatures(ImmutableMap.of(Feature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS.getFlag(), false));

    // it does not evaluate the SCM repository content and it returns null
    assertThat(service.doSynchronousSourceControlScan(APP_ID, new Stage(Stage.ID_DEVELOP), "testBranchName")).isNull();
  }
}

