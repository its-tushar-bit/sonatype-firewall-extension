/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.HashMap;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.git.GitApiFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;

import com.google.inject.Binder;
import org.junit.Test;

import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourceControlRepositoryUtilsTest
    extends AbstractComponentTest
{
  @Inject
  private SourceControlRepositoryUtils sourceControlRepositoryUtils;

  private GitApiFactory gitApiFactory;

  private final GitApi mockGitApiInstance = mock(GitApi.class);

  @Override
  public void configure(Binder binder) {
    gitApiFactory = mock(GitApiFactory.class);
    binder.bind(GitApiFactory.class).toInstance(gitApiFactory);
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
}
