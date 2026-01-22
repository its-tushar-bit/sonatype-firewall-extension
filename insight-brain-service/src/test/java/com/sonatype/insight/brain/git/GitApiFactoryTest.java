/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.JGitApi;
import com.sonatype.nexus.git.utils.api.NativeGitApi;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GitApiFactoryTest
    extends AbstractComponentTest
{
  private static final String GIT_EXECUTABLE = "/usr/bin/git";

  private static final GitRepositoryInfo GIT_REPOSITORY_INFO = new GitRepositoryInfo("localhost", null, null, "token",
      SourceControlProvider.GITHUB, "master", true, true, true, true, true, true, false, null);

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Inject
  private GitApiFactory gitApiFactory;
  
  @Inject
  private Configuration configuration;

  @Inject
  private PasswordHandler passwordHandler;

  private GitApiFactory spyGitApiFactory;

  @Before
  public void setup() {
    // Note usage of spy in order to override isNativeGitAvailable
    spyGitApiFactory = spy(gitApiFactory);
  }

  @Test
  public void test_noNativeAvailable_noConfig() {
    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_nativeAvailable_forceViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.JAVA);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_nativeAvailable_noConfig() {
    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }

  @Test
  public void test_nativeAvailable_gitExecutable_noConfig() {
    assumeThat(new File(GIT_EXECUTABLE)).exists();
    when(spyGitApiFactory.isNativeGitAvailable(GIT_EXECUTABLE)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitExecutable(GIT_EXECUTABLE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }

  @Test
  public void test_noNativeAvailable_forceNativeViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_nativeAvailable_forceJavaViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }

  @Test
  public void test_NativeGit_gitTimeoutViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitTimeoutSeconds(600);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("timeout").isEqualTo(600);
  }

  @Test
  public void test_JGit_gitTimeoutViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitTimeoutSeconds(600);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("timeout").isEqualTo(600);
  }

  @Test
  public void test_noNativeAvailable_gitExecutable_forceViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(GIT_EXECUTABLE)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfiguration.setGitExecutable(GIT_EXECUTABLE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_sshEnabledButNoSshUrl() {
    GitRepositoryInfo sshGitRepositoryInfo = new GitRepositoryInfo("localhost", null, null, "token",
        SourceControlProvider.GITHUB, "master", true, true, true, true, true, true, true, null);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    assertThatThrownBy(() -> spyGitApiFactory.createGitApi(sshGitRepositoryInfo))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("SSH is enabled for repository")
        .hasMessageContaining("but no SSH clone URL was");
  }

  @Test
  public void test_sshEnabledWithSshUrl() {
    String sshUrl = "git@github.com:foo/bar.git";
    GitRepositoryInfo sshGitRepositoryInfo = new GitRepositoryInfo("localhost", sshUrl, null,
        "token", SourceControlProvider.GITHUB, "master", true,true, true, true, true, true, true, null);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(sshGitRepositoryInfo);
    assertThat(gitApi).hasFieldOrPropertyWithValue("repositoryUrl", sshUrl);
  }

  @Test
  public void test_sshEnabledButJgitConfigured() {
    String sshUrl = "git@github.com:foo/bar.git";
    GitRepositoryInfo sshGitRepositoryInfo = new GitRepositoryInfo("localhost", sshUrl, null, "token",
        SourceControlProvider.GITHUB, "master", true, true,true, true, true, true, true, null);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.JAVA);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    assertThatThrownBy(() -> spyGitApiFactory.createGitApi(sshGitRepositoryInfo))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Application with URL " + sshUrl + " is configured to use SSH with JGit");
  }

  @Test
  public void test_JGit_gpgSigningKeyViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-gpg-key-id");
    sourceControlConfiguration.setGpgPassphrase(passwordHandler.encryptPassword("test-passphrase"));
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgSigningKey").isEqualTo("test-gpg-key-id");
    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgPassphrase").isEqualTo("test-passphrase".toCharArray());
  }

  @Test
  public void test_NativeGit_gpgSigningKeyViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-gpg-key-id");
    sourceControlConfiguration.setGpgPassphrase(passwordHandler.encryptPassword("test-passphrase"));
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("gpgSigningKey").isEqualTo("test-gpg-key-id");
    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("gpgPassphrase").isEqualTo("test-passphrase");
  }

  @Test
  public void test_JGit_nullGpgPassphraseViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-gpg-key-id");
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgSigningKey").isEqualTo("test-gpg-key-id");
    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgPassphrase").isNull();
  }

  @Test
  public void test_JGit_nullGpgInfoViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgSigningKey").isNull();
    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgPassphrase").isNull();
  }

  @Test
  public void test_NativeGit_nullGpgInfoViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("gpgSigningKey").isNull();
    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("gpgPassphrase").isNull();
  }
}
