/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.SourceControlConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.nexus.git.utils.api.GitApi;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.APPLICATION_EVALUATION_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.MANIFEST_SCAN_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.STATUS_UPDATE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManifestScanServiceTest
    extends VerifiableLoggingTestBase
{
  private static final String APP_ID = "app-id";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Mock
  private InsightConfig mockInsightConfig;

  @Mock
  private GitApiFactory mockGitApiFactory;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private ApplicationDAO mockApplicationDAO;

  @Mock
  private GitRepositoryInfo mockGitRepositoryInfo;

  @Mock
  private GitApi mockGitApi;

  @Mock
  private SourceControlEventService sourceControlEventService;

  @Mock
  private PolicyEvaluateService policyEvaluateService;

  @Mock
  private ProprietaryConfigService proprietaryConfigService;

  @Mock
  private InsightWork work;

  @Mock
  private Scanner scanner;

  private SourceControlConfig sourceControlConfig;

  private Application application;

  private SourceControlEvent sourceControlEvent;

  private File sourceControlDir;

  private ScanResult scanResult;

  private ProprietaryConfig proprietaryConfig;

  // subject
  private ManifestScanService service;

  public ManifestScanServiceTest() {
    super(ManifestScanService.class);
  }

  @Before
  @Override
  public void setup() {
    super.setup();

    service = new ManifestScanService(
        mockInsightConfig, mockGitApiFactory, mockSourceControlUtils, mockApplicationDAO, sourceControlEventService,
        proprietaryConfigService, policyEvaluateService, work, scanner);

    try {
      sourceControlDir = tmpDir.newFolder();
    }
    catch (final IOException ioEx) {
      throw new RuntimeException("failed creating temp source control dir", ioEx);
    }

    sourceControlConfig = new SourceControlConfig();
    sourceControlConfig.setCloneDirectory(sourceControlDir.getAbsolutePath());

    application = new Application();
    application.setPublicId("public-app-id");
    when(mockApplicationDAO.getByIdNotNull(eq(APP_ID))).thenReturn(application);

    sourceControlEvent = new SourceControlEvent();
    sourceControlEvent.setApplicationId(APP_ID);

    proprietaryConfig = new ProprietaryConfig();
    when(proprietaryConfigService.getProprietaryConfig(eq(OwnerType.APPLICATION), eq("public-app-id")))
        .thenReturn(proprietaryConfig);
  }

  @Test
  public void testOnManifestScan_WithNoSourceControl() throws Exception {
    // given there is no source control info for an application
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId()))
        .thenReturn(null);

    // when we receive a manifest scan event
    service.onManifestScan(sourceControlEvent);

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

    // and an application
    when(mockApplicationDAO.getById(sourceControlEvent.getApplicationId())).thenReturn(application);

    // and a source control configuration
    when(mockInsightConfig.getSourceControl()).thenReturn(sourceControlConfig);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId()))
        .thenReturn(mockGitRepositoryInfo);
    when(mockGitApiFactory.createGitApi(mockGitRepositoryInfo)).thenReturn(mockGitApi);

    // and a scan result
    scanResult = new ScanResult();
    File scanDir = mock(File.class);
    scanResult.setScanFile(mock(File.class));
    when(work.getScanDir(eq(APP_ID))).thenReturn(scanDir);
    when(scanner.scan(any(File.class), isNull(), eq(scanDir), eq(proprietaryConfig))).thenReturn(scanResult);

    // when we receive a manifest scan event
    service.onManifestScan(sourceControlEvent);

    // then it creates the target directory
    assertThat(Arrays.stream(Objects.requireNonNull(sourceControlDir.list())).anyMatch(filename ->
        filename.startsWith("public-app-id-branch-"))).isTrue();

    // and it calls the repository sync
    verify(mockGitApi).cloneOrPullRepository(isA(File.class), eq(sourceControlEvent.getBranchName()));

    // and it evaluates a policy
    verify(policyEvaluateService).evaluateWithPolling(eq("statusId"),
        isA(Application.class), eq(ClientScanType.SONATYPE), argThat(s -> s.getStageTypeId().equals(Stage.ID_DEVELOP)),
        isA(File.class), eq("api"),
        eq("userAgent"));
  }

  @Test
  public void testExecuteEvent_skips() throws Exception {
    // given a list of all event types to skip
    List<String> skipEventTypes = Arrays.asList(APPLICATION_EVALUATION_EVENT, DISCOVERED_PULL_REQUEST_EVENT,
        REMEDIATION_PULL_REQUEST_EVENT, STATUS_UPDATE_EVENT);

    // when we receive a manifest scan event that we do not handle, receive false
    for (String eventType : skipEventTypes) {
      SourceControlEvent event = new SourceControlEvent();
      event.setEventType(eventType);
      assertThat(service.executeEvent(event)).isFalse();
    }
  }

  @Test
  public void testExecuteEvent_handles() throws Exception {
    // given a list of all event types to respond to
    List<String> handleEventTypes = Arrays.asList(MANIFEST_SCAN_EVENT);

    // then ensure that executeEvent is true
    for (String eventType : handleEventTypes) {
      SourceControlEvent event = new SourceControlEvent();
      event.setEventType(eventType);
      assertThat(service.executeEvent(event)).isTrue();
    }
  }
}

