/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.SourceControlConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManifestScanServiceTest
    extends VerifiableLoggingTestBase
{
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

  private SourceControlConfig sourceControlConfig;

  private Application application;

  private SourceControlEvent sourceControlEvent;

  private File sourceControlDir;

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
        mockInsightConfig, mockGitApiFactory, mockSourceControlUtils, mockApplicationDAO);

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

    sourceControlEvent = new SourceControlEvent();
    sourceControlEvent.setApplicationId("app-id");
  }

  @Test
  public void testOnManifestScan_WithNoSourceControl() throws GitException {
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
  public void testOnManifestScan_WithBranch() throws GitException {
    // given a branch is provided
    sourceControlEvent.setBranchName("branch");

    // and an application
    when(mockApplicationDAO.getById(sourceControlEvent.getApplicationId())).thenReturn(application);

    // and a source control configuration
    when(mockInsightConfig.getSourceControl()).thenReturn(sourceControlConfig);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId()))
        .thenReturn(mockGitRepositoryInfo);
    when(mockGitApiFactory.createGitApi(mockGitRepositoryInfo)).thenReturn(mockGitApi);

    // when we receive a manifest scan event
    service.onManifestScan(sourceControlEvent);

    // then it creates the target directory
    assertThat(Arrays.stream(Objects.requireNonNull(sourceControlDir.list())).anyMatch(filename ->
        filename.startsWith("public-app-id-branch-"))).isTrue();

    // and it calls the repository sync
    verify(mockGitApi).cloneOrPullRepository(isA(File.class), eq(sourceControlEvent.getBranchName()));
  }

  @Test
  public void testOnManifestScan_WithNoBranch() throws GitException {
    // given a branch is not provided
    sourceControlEvent.setBranchName(null);

    // and an application
    when(mockApplicationDAO.getById(sourceControlEvent.getApplicationId())).thenReturn(application);

    // and a source control configuration
    when(mockInsightConfig.getSourceControl()).thenReturn(sourceControlConfig);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(sourceControlEvent.getApplicationId()))
        .thenReturn(mockGitRepositoryInfo);
    when(mockGitApiFactory.createGitApi(mockGitRepositoryInfo)).thenReturn(mockGitApi);
    when(mockGitRepositoryInfo.getBaseBranch()).thenReturn("default-branch");

    // when we receive a manifest scan event
    service.onManifestScan(sourceControlEvent);

    // then it creates the target directory
    assertThat(Arrays.stream(Objects.requireNonNull(sourceControlDir.list())).anyMatch(filename ->
        filename.startsWith("public-app-id-default-branch-"))).isTrue();

    // and it call the repository sync
    verify(mockGitApi).cloneOrPullRepository(isA(File.class), eq("default-branch"));
  }
}

