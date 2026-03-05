/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.HashMap;

import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.git.GitApiFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlRepositoryUtilsTest
    extends AbstractComponentTest
{
  @Inject
  private SourceControlRepositoryUtils sourceControlRepositoryUtils;

  private GitApiFactory gitApiFactory;

  private GitHubAppDAO mockGitHubAppDAO;

  private final GitApi mockGitApiInstance = mock(GitApi.class);

  @Override
  public void configure(Binder binder) {
    gitApiFactory = mock(GitApiFactory.class);
    mockGitHubAppDAO = mock(GitHubAppDAO.class);
    binder.bind(GitApiFactory.class).toInstance(gitApiFactory);
    binder.bind(GitHubAppDAO.class).toInstance(mockGitHubAppDAO);
    super.configure(binder);
  }

  @Test
  public void testGetRepositoryHttpUrlFromSshUrl() {
    assertThat(
        sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl("git@github.com:username/repo-name.git")).isEqualTo(
        "https://github.com/username/repo-name.git");

    assertThat(sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl(
        "git@gitlab.com:username/repository.git")).isEqualTo("https://gitlab.com/username/repository.git");

    assertThat(sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl(
        "git@gitlab.com:my-group1153/my-sub-group/my-sub-sub-group/my-sub-sub-group-project.git")).isEqualTo(
        "https://gitlab.com/my-group1153/my-sub-group/my-sub-sub-group/my-sub-sub-group-project.git");

    assertThat(sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl(
        "git@bitbucket.org:username/repository.git")).isEqualTo("https://bitbucket.org/username/repository.git");

    assertThat(sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl(
        "git@ssh.dev.azure.com:v3/username/project/repository")).isEqualTo(
        "https://dev.azure.com/username/project/_git/repository");
  }

  @Test
  public void testGetRepositoryHttpUrlFromSshUrl_derivedUrlNull() {
    assertThat(sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl(null)).isNull();

    assertThat(sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl("no-match")).isNull();

    assertThat(sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl(
        "https://github.com/already/https")).isNull();

    assertThat(sourceControlRepositoryUtils.getRepositoryHttpUrlFromSshUrl(
        "git@customdomain.com:username/repo-name.git")).isNull();
  }

  @Test
  public void testIsRepositoryReachable() throws Exception {
    String repositoryUrl = "http://github.com/username/repository";

    Organization organization = tempEntity.newOrganization();
    tempEntity.newSourceControl(organization.getId(), null, "token", GITHUB);
    Application application = tempEntity.newApplicationWithParent(organization);

    HashMap<String, String> headCommitsForAllBranches = new HashMap<>();
    headCommitsForAllBranches.put("main", "data");

    when(mockGitApiInstance.getHeadCommitsForAllBranches(repositoryUrl)).thenReturn(headCommitsForAllBranches);
    when(gitApiFactory.createGitApi(any())).thenReturn(mockGitApiInstance);

    assertThat(sourceControlRepositoryUtils.isRepositoryReachable(application, repositoryUrl)).isTrue();
  }

  @Test
  public void testIsRepositoryReachable_False() throws Exception {
    String repositoryUrl = "http://github.com/username/repository";

    Organization organization = tempEntity.newOrganization();
    tempEntity.newSourceControl(organization.getId(), null, "token", GITHUB);
    Application application = tempEntity.newApplicationWithParent(organization);

    when(mockGitApiInstance.getHeadCommitsForAllBranches(repositoryUrl)).thenThrow(new GitException(""));
    when(gitApiFactory.createGitApi(any())).thenReturn(mockGitApiInstance);

    assertThat(sourceControlRepositoryUtils.isRepositoryReachable(application, repositoryUrl)).isFalse();
  }

  @Test
  public void testIsRepositoryReachable_WithGitHubApp_BuildsCorrectGitInfo() throws Exception {
    String repositoryUrl = "http://github.com/username/repository";

    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplicationWithParent(organization);

    SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(organization.getId());
    sourceControl.setProvider(GITHUB);
    sourceControl.setAuthenticationType(SourceControl.AuthenticationType.GITHUB_APP);
    tempEntity.newSourceControl(sourceControl);

    GitHubApp mockGitHubApp = new GitHubApp();
    mockGitHubApp.setOwnerId(organization.getId());
    mockGitHubApp.setInstallationId(12345L);

    when(mockGitHubAppDAO.getNearestGitHubApp(application.getId())).thenReturn(mockGitHubApp);

    HashMap<String, String> headCommits = new HashMap<>();
    headCommits.put("main", "commit-sha");
    when(mockGitApiInstance.getHeadCommitsForAllBranches(repositoryUrl)).thenReturn(headCommits);

    ArgumentCaptor<GitRepositoryInfo> captor = ArgumentCaptor.forClass(GitRepositoryInfo.class);
    when(gitApiFactory.createGitApi(captor.capture())).thenReturn(mockGitApiInstance);

    boolean reachable = sourceControlRepositoryUtils.isRepositoryReachable(application, repositoryUrl);

    assertThat(reachable).isTrue();

    GitRepositoryInfo capturedInfo = captor.getValue();
    assertThat(capturedInfo.authenticationType).isEqualTo(SourceControl.AuthenticationType.GITHUB_APP);
    assertThat(capturedInfo.authOwnerId).isEqualTo(organization.getId());

    verify(mockGitHubAppDAO).getNearestGitHubApp(application.getId());
  }

  @Test
  public void testIsRepositoryReachable_WithGitHubApp_NoGitHubAppFound() throws Exception {
    String repositoryUrl = "http://github.com/username/repository";

    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplicationWithParent(organization);

    SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(organization.getId());
    sourceControl.setProvider(GITHUB);
    sourceControl.setAuthenticationType(SourceControl.AuthenticationType.GITHUB_APP);
    tempEntity.newSourceControl(sourceControl);

    when(mockGitHubAppDAO.getNearestGitHubApp(application.getId())).thenReturn(null);

    when(mockGitApiInstance.getHeadCommitsForAllBranches(repositoryUrl)).thenReturn(new HashMap<>());

    ArgumentCaptor<GitRepositoryInfo> captor = ArgumentCaptor.forClass(GitRepositoryInfo.class);
    when(gitApiFactory.createGitApi(captor.capture())).thenReturn(mockGitApiInstance);

    boolean reachable = sourceControlRepositoryUtils.isRepositoryReachable(application, repositoryUrl);

    assertThat(reachable).isFalse();

    GitRepositoryInfo capturedInfo = captor.getValue();
    // When GitHub App auth is configured but no app is found, authOwnerId falls back to sourceControl.ownerId
    assertThat(capturedInfo.authOwnerId).isEqualTo(organization.getId());
  }

  @Test
  public void testIsRepositoryReachable_WithToken_DoesNotPopulateAuthOwnerId() throws Exception {
    String repositoryUrl = "http://github.com/username/repository";

    Organization organization = tempEntity.newOrganization();
    tempEntity.newSourceControl(organization.getId(), null, "token", GITHUB);
    Application application = tempEntity.newApplicationWithParent(organization);

    HashMap<String, String> headCommits = new HashMap<>();
    headCommits.put("main", "commit-sha");
    when(mockGitApiInstance.getHeadCommitsForAllBranches(repositoryUrl)).thenReturn(headCommits);

    ArgumentCaptor<GitRepositoryInfo> captor = ArgumentCaptor.forClass(GitRepositoryInfo.class);
    when(gitApiFactory.createGitApi(captor.capture())).thenReturn(mockGitApiInstance);

    boolean reachable = sourceControlRepositoryUtils.isRepositoryReachable(application, repositoryUrl);

    assertThat(reachable).isTrue();

    GitRepositoryInfo capturedInfo = captor.getValue();
    // When using PAT authentication, authOwnerId is populated with sourceControl.ownerId
    assertThat(capturedInfo.authOwnerId).isEqualTo(organization.getId());

    verify(mockGitHubAppDAO, never()).getNearestGitHubApp(any());
  }
}
