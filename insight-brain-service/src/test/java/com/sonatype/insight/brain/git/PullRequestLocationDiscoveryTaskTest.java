/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.SourceControlConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.location.discovery.LocationDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestLocationDiscoveryTaskTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES =
      ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0.0", "", "jar");

  private static final String BRANCH = "testBranch";

  private static final String APP_INTERNAL_ID = "8f9a4a2973804402ab5c6bd0ee453ed9";

  private static final String APP_PUBLIC_ID = "sandbox-application";

  private static final String APP_HASH = "-khbn98"; // hash of APP_INTERNAL_ID with leading dash

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(PullRequestLocationDiscoveryTask.class, GitRepositoryTask.class);

  @Mock
  private GitApiFactory gitApiFactory;

  @Mock
  private InsightConfig insightConfig;
  
  @Mock 
  private FileCleaner fileCleaner;
  
  @Mock
  private Application app;
  
  @Mock
  private GitApi gitApi;
  
  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private LocationDiscoveryExecutor locationDiscoveryExecutor;

  @Mock
  private LocationDiscoveryResult locationDiscoveryResult;

  //Subject
  private PullRequestLocationDiscoveryTask locationDiscoveryTask;

  private List<ComponentIdentifier> identifierList;

  public static final GitRepositoryInfo INFO =
      new GitRepositoryInfo("localhost", null, "token", SourceControlProvider.GITHUB, "master", true, true);

  @Before
  public void setup() {
    locationDiscoveryTask =
        new PullRequestLocationDiscoveryTask(applicationDAO, gitApiFactory, insightConfig, fileCleaner);
    identifierList = new LinkedList<>();
    identifierList.add(MAVEN_COORDINATES);
  }
  
  @Test
  public void testCall_notInitialized() {
    // given: task is not initialized

    // when: try running the task
    locationDiscoveryTask.call();

    // then: an error is logged and execution is aborted
    assertThat(logOutput).atErrorLevel().contains("Missing required locationDiscoveryExecutor");
    verifyNoInteractions(applicationDAO, gitApiFactory, insightConfig, fileCleaner);
  }

  @Test
  public void testCall_success() throws Exception {
    // given: task is properly initialized
    SourceControlConfig config = new SourceControlConfig();
    File sonatypeWorkDir = temporaryFolder.newFolder();
    config.setSonatypeWorkDir(sonatypeWorkDir);

    File targetDirectory = new File(config.getCloneDirectory(), APP_PUBLIC_ID + "-" + INFO.baseBranch + APP_HASH);
    boolean success = targetDirectory.mkdirs();
    assertThat(success).isTrue();

    configureExpectations(config);
    when(locationDiscoveryExecutor.execute(any())).thenReturn(locationDiscoveryResult);

    locationDiscoveryTask.init(locationDiscoveryExecutor, identifierList, INFO, BRANCH, APP_INTERNAL_ID);

    // when: try running the task
    locationDiscoveryTask.call();

    // then: the task is successfully executed
    verify(locationDiscoveryExecutor).execute(any());
    assertThat(logOutput).atDebugLevel().contains("Pull request location discovery task initiated for application");
    assertThat(logOutput).atDebugLevel().contains("Pull request location discovery task completed for application");
  }
  
  @Test
  public void testCall_failure() throws Exception {
    // given: task is properly initialized
    SourceControlConfig config = new SourceControlConfig();
    File sonatypeWorkDir = temporaryFolder.newFolder();
    config.setSonatypeWorkDir(sonatypeWorkDir);

    File targetDirectory = new File(config.getCloneDirectory(), APP_PUBLIC_ID + "-" + INFO.baseBranch + APP_HASH);
    boolean success = targetDirectory.mkdirs();
    assertThat(success).isTrue();

    configureExpectations(config);

    // but locationDiscoveryExecutor fails
    when(locationDiscoveryExecutor.execute(any())).thenThrow(new GitException("Something bad happened"));

    locationDiscoveryTask.init(locationDiscoveryExecutor, identifierList, INFO, BRANCH, APP_INTERNAL_ID);

    // when: try running the task
    locationDiscoveryTask.call();

    // then: an error is logged and teh checkout directory is deleted
    verify(fileCleaner).delete(targetDirectory);
    assertThat(logOutput).atErrorLevel()
        .contains("Failed to execute pull request location discovery task, cleaning pull request directory");
  }

  private void configureExpectations(final SourceControlConfig config) {
    when(insightConfig.getSourceControl()).thenReturn(config);
    when(gitApiFactory.createGitApi(INFO)).thenReturn(gitApi);
    when(app.getPublicId()).thenReturn(APP_PUBLIC_ID);
    when(applicationDAO.getById(APP_INTERNAL_ID)).thenReturn(app);
  }
}
