/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.SourceControlConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.JGitApi;
import com.sonatype.nexus.git.utils.api.NativeGitApi;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.git.GitApiFactory.JGIT;
import static com.sonatype.insight.brain.git.GitApiFactory.NATIVE_GIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitApiFactoryTest
{
  private static final String GIT_EXECUTABLE = "/usr/bin/git";

  @Mock
  private InsightConfig insightConfig;

  @Mock
  private SourceControlConfig sourceControlConfig;

  private final GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo("localhost", null, "token",
      SourceControlProvider.GITHUB, "master", true, true);

  private GitApiFactory gitApiFactory;

  @Before
  public void setup() {
    when(insightConfig.getSourceControl()).thenReturn(sourceControlConfig);

    // Note usage of spy in order to override isNativeGitAvailable
    gitApiFactory = spy(new GitApiFactory(insightConfig));
  }

  @Test
  public void test_badInstantiation() {
    when(insightConfig.getSourceControl()).thenReturn(null);
    assertThatThrownBy(() -> {
      new GitApiFactory(insightConfig);
    }).isInstanceOf(NullPointerException.class).hasMessageContaining("sourceControl in InsightConfig cannot be null");
  }

  @Test
  public void test_noNativeAvailable_noConfig() {
    when(gitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    when(sourceControlConfig.getGitImplementation()).thenReturn(null);

    GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_nativeAvailable_forceViaConfig() {
    lenient().when(gitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    when(sourceControlConfig.getGitImplementation()).thenReturn(JGIT);

    GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_nativeAvailable_noConfig() {
    when(gitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    when(sourceControlConfig.getGitImplementation()).thenReturn(null);

    GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }

  @Test
  public void test_nativeAvailable_gitExecutable_noConfig() {
    assumeThat(new File(GIT_EXECUTABLE)).exists();

    when(gitApiFactory.isNativeGitAvailable(GIT_EXECUTABLE)).thenReturn(true);
    when(sourceControlConfig.getGitImplementation()).thenReturn(null);
    when(sourceControlConfig.getGitExecutable()).thenReturn(GIT_EXECUTABLE);

    GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }

  @Test
  public void test_noNativeAvailable_forceNativeViaConfig() {
    lenient().when(gitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    when(sourceControlConfig.getGitImplementation()).thenReturn(NATIVE_GIT);

    GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_nativeAvailable_forceJavaViaConfig() {
    lenient().when(gitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    when(sourceControlConfig.getGitImplementation()).thenReturn(NATIVE_GIT);

    GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }

  @Test
  public void test_noNativeAvailable_gitExecutable_forceViaConfig() {
    lenient().when(gitApiFactory.isNativeGitAvailable(GIT_EXECUTABLE)).thenReturn(false);
    when(sourceControlConfig.getGitImplementation()).thenReturn(NATIVE_GIT);
    when(sourceControlConfig.getGitExecutable()).thenReturn(GIT_EXECUTABLE);

    GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_unknownConfig_defaultToWhatIsAvailable() {
    when(gitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    when(sourceControlConfig.getGitImplementation()).thenReturn("badconfig");

    GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }
}
