/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.io.File;
import java.nio.file.Files;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlUtilsTest
    extends AbstractComponentTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  private static final String TOKEN = "token";

  private ApiSourceControlService mockSourceControlService;

  private GitClientFactory mockGitClientFactory;

  private GitApiClient mockGitClientApi;

  private Application application;

  private Organization org;

  @Inject
  private SourceControlUtils sourceControlUtils;

  @Inject
  private InsightWork insightWork;

  @Override
  public void configure(Binder binder) {
    mockSourceControlService = mock(ApiSourceControlService.class);
    mockGitClientFactory = mock(GitClientFactory.class);
    mockGitClientApi = mock(GitApiClient.class);
    binder.bind(ApiSourceControlService.class).toInstance(mockSourceControlService);
    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);
    binder.bind(GitApiClient.class).toInstance(mockGitClientApi);
    super.configure(binder);
  }

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    application = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_ProviderAndTokenFromApplication() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getParentOwnerId())
        .setRepositoryUrl(VALID_URL)
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.GITHUB)
        .setBaseBranch("base-branch")
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getId()))).thenReturn(sourceControl);

    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());

    assertThat(value).isNotNull();
    assertThat(value.token).isEqualTo(TOKEN);
    assertThat(value.provider).isEqualTo(SourceControlProvider.GITHUB);
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(application.getId());
    verify(mockSourceControlService, never()).getSourceControlByOwnerDecrypted(application.getOrganizationId());
    verify(mockSourceControlService, never()).getSourceControlByOwnerDecrypted(Organization.ROOT_ORGANIZATION_ID);

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isTrue();
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_ProviderAndTokenFromOrganization() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .setBaseBranch("base-branch")
        .build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getId()))).thenReturn(sourceControl);

    SourceControl orgSourceControl = new SourceControl.Builder()
        .setOwnerId(org.getId())
        .setRepositoryUrl(null)
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.GITHUB)
        .build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getOrganizationId())))
        .thenReturn(orgSourceControl);

    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());

    assertThat(value).isNotNull();
    assertThat(value.token).isEqualTo(TOKEN);
    assertThat(value.provider).isEqualTo(SourceControlProvider.GITHUB);
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(application.getId());
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(application.getOrganizationId());
    verify(mockSourceControlService, never()).getSourceControlByOwnerDecrypted(org.getParentOrganizationId());

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isTrue();
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_ProviderAndTokenFromRootOrganization() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken(null)
        .setProvider(null)
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .setBaseBranch("base-branch")
        .build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getOrganizationId())))
        .thenReturn(null);

    SourceControl rootOrgSourceControl = new SourceControl.Builder()
        .setOwnerId(org.getParentOrganizationId())
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.GITHUB)
        .build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(Organization.ROOT_ORGANIZATION_ID)))
        .thenReturn(rootOrgSourceControl);

    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());

    assertThat(value.token).isEqualTo(TOKEN);
    assertThat(value.provider).isEqualTo(SourceControlProvider.GITHUB);
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(application.getId());
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(org.getParentOrganizationId());

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isTrue();
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_defaultBranch() {
    // given : source control for app and root with null base branch
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setBaseBranch(null)
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .build();

    SourceControl rootOrgSourceControl = new SourceControl.Builder()
        .setOwnerId(org.getParentOrganizationId())
        .setToken(TOKEN)
        .setBaseBranch(null)
        .setProvider(SourceControlProvider.GITHUB)
        .build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getOrganizationId())))
        .thenReturn(null);

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(Organization.ROOT_ORGANIZATION_ID)))
        .thenReturn(rootOrgSourceControl);

    // when : get repo info for app
    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());

    // then : expect result to have default base branch
    assertThat(value.baseBranch).isEqualTo(SourceControlUtils.DEFAULT_BASE_BRANCH);
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(application.getId());
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(eq(application.getOrganizationId()));

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isTrue();
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_NoApplicationSourceControl() {
    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForApplication("INVALID");
    assertThat(value).isNull();
    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();
  }

  @Test
  public void testIsScmEnabled_NoRepositoryUrl() {
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(application.getId()).build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getId()))).thenReturn(sourceControl);

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();
  }

  @Test
  public void testIsScmEnabled_NoProvider() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();
  }

  @Test
  public void testIsScmEnabled_NoToken() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getOrganizationId())))
        .thenReturn(null);

    SourceControl rootOrgSourceControl = new SourceControl.Builder()
        .setOwnerId(org.getParentOrganizationId())
        .setProvider(SourceControlProvider.GITHUB)
        .build();

    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(Organization.ROOT_ORGANIZATION_ID)))
        .thenReturn(rootOrgSourceControl);

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();
  }

  @Test
  public void testIsScmEnabled_RequiresUsername() {
    // given: source control is setup for a provider that requires username, but does not set the username
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getParentOwnerId())
        .setRepositoryUrl(VALID_URL)
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.BITBUCKET)
        .setBaseBranch("base-branch")
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .build();
    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getId()))).thenReturn(sourceControl);

    // expect: source control is not enabled
    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();

    // given: we provide a username
    sourceControl.setUsername("username");

    // expect: source control is enabled
    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isTrue();
  }

  @Test
  public void testGetCheckoutDirectory() {
    File expectedSourceControlDir = insightWork.getSourceControlDir(application.getId());
    assertThat(expectedSourceControlDir).doesNotExist();

    assertThat(sourceControlUtils.getCheckoutDirectory(application)).isEqualTo(expectedSourceControlDir);
    assertThat(expectedSourceControlDir).isDirectory();
  }

  @Test
  public void testDeleteCheckoutDirectory() throws Exception {
    File expectedSourceControlDir = insightWork.getSourceControlDir(application.getId());
    Files.createDirectories(expectedSourceControlDir.toPath());
    new File(expectedSourceControlDir, "foo.txt").createNewFile();

    sourceControlUtils.deleteCheckoutDirectory(application);
    assertThat(expectedSourceControlDir).doesNotExist();
  }

  @Test
  public void testIsBitbucketCloud() {
    // BB cloud
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("https://bitbucket.org/organization/project", "user", TOKEN, BITBUCKET,
            "base-branch", true, true);
    assertThat(sourceControlUtils.isBitbucketCloud(gitRepositoryInfo)).isTrue();

    // BB server
    gitRepositoryInfo =
        new GitRepositoryInfo("https://my.domain.com/organization/project", "user", TOKEN, BITBUCKET,
            "base-branch", true, true);
    assertThat(sourceControlUtils.isBitbucketCloud(gitRepositoryInfo)).isFalse();

    // Not BB
    gitRepositoryInfo =
        new GitRepositoryInfo("https://my.domain.com/organization/project", "user", TOKEN, GITHUB,
            "base-branch", true, true);
    assertThat(sourceControlUtils.isBitbucketCloud(gitRepositoryInfo)).isFalse();
  }

  @Test
  public void testGetUserId() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getParentOwnerId())
        .setRepositoryUrl(VALID_URL)
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.GITHUB)
        .setBaseBranch("base-branch")
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .build();

    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitClientApi);
    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(application.getId()))).thenReturn(sourceControl);
    when(mockGitClientApi.getUserId()).thenReturn("scmUser");

    String userId = sourceControlUtils.getScmUserIdForApplication(application.getId());

    verify(mockSourceControlService, times(1)).getSourceControlByOwnerDecrypted(application.getId());
    verify(mockGitClientFactory, times(1)).createApiClient(any());
    verify(mockGitClientApi, times(1)).getUserId();
    assertThat(userId).isNotNull();
    assertThat(userId).isEqualTo("scmUser");
  }
}
