/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScmUserMatchingServiceTest
    extends AbstractComponentTest
{
  private Organization org;

  private Application app;

  private final String repoUrl = "https://github.com/myAwesomeOrg/myAwesomeRepo";

  private final String bitbucketRepoUrl = "https://bitbucket.com/scm/myAwesomeOrg/myAwesomeRepo";

  @Inject
  private ScmUserMatchingService scmUserMatchingService;

  @Inject
  private MembershipMappingDAO membershipMappingDAO;

  @Inject
  private RoleDAO roleDAO;

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Override
  public void configure(final Binder binder) {
    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);
    binder.bind(SourceControlUtils.class).toInstance(mockSourceControlUtils);
    super.configure(binder);
  }

  @Before
  public void setup() throws Exception {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(org.getParentOrganizationId(), null, "token", SourceControlProvider.GITHUB);
  }

  @Test
  public void testAutomaticRoleAssignment_BadPublicId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> scmUserMatchingService.automaticRoleAssignment("something"))
        .withMessageContaining("Could not find an application with public ID something.");
  }

  @Test
  public void testAutomaticRoleAssignment_NoSourceControlForApp() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> scmUserMatchingService.automaticRoleAssignment(app.getPublicId()))
        .withMessageContaining("Cannot find GitRepositoryInfo for " + app.getPublicId());
  }

  @Test
  public void testAutomaticRoleAssignment_Success() {
    tempEntity.newSourceControl(app.getId(), repoUrl);
    tempEntity.newUser("myuser");
    tempEntity.newUser("otheruser");

    Set<String> githubUsers = new HashSet<>();
    githubUsers.add("myuser");
    githubUsers.add("otheruser");
    githubUsers.add("unknownuser");

    mockGithubClientAndScmUtils(app, githubUsers);
    Set<String> usersFound = scmUserMatchingService.automaticRoleAssignment(app.getPublicId());
    List<MembershipMapping> createdMembershipMappings = getDeveloperMembershipMappings();

    assertThat(usersFound).hasSize(2);
    assertThat(usersFound).containsExactlyInAnyOrder("myuser", "otheruser");
    assertThat(createdMembershipMappings.get(0).getMemberName()).isEqualTo("myuser");
    assertThat(createdMembershipMappings.get(1).getMemberName()).isEqualTo("otheruser");
  }

  @Test
  public void testAutomaticRoleAssignment_EmptyResult() {
    tempEntity.newSourceControl(app.getId(), repoUrl);
    tempEntity.newUser("myuser");
    tempEntity.newUser("otheruser");

    Set<String> githubUsers = new HashSet<>();
    githubUsers.add("unknownmyuser");
    githubUsers.add("unknownotheruser");
    githubUsers.add("unknownuser");

    mockGithubClientAndScmUtils(app, githubUsers);
    Set<String> usersFound = scmUserMatchingService.automaticRoleAssignment(app.getPublicId());

    assertThat(usersFound).hasSize(0);
  }

  @Test
  public void testAutomaticRoleAssignment_SCMProviderNotSupported() {
    tempEntity.newSourceControl(app.getId(), bitbucketRepoUrl, "token",
        SourceControlProvider.BITBUCKET);
    tempEntity.newUser("myuser");
    tempEntity.newUser("otheruser");

    mockBitbucketClientAndScmUtils(app);
    assertThatThrownBy(() -> scmUserMatchingService.automaticRoleAssignment(app.getPublicId()))
        .isInstanceOf(RuntimeException.class).hasMessage("There was an error communicating with SCM");
  }

  private void mockGithubClientAndScmUtils(Application app, Set<String> githubUsers) {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo();
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(app.getId())).thenReturn(gitRepositoryInfo);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(mockGitClientFactory.createApiClient(gitRepositoryInfo)).thenReturn(mockClient);
    try {
      when(mockClient.getRepositoryContributorsUsernames()).thenReturn(githubUsers);
    }
    catch (IOException e) {
      fail("Exception thrown when trying to mock getRepositoryContributorsUsernames call", e);
    }
  }

  private void mockBitbucketClientAndScmUtils(Application app) {
    GitRepositoryInfo gitRepositoryInfo = getBitbucketRepositoryInfo();
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(app.getId())).thenReturn(gitRepositoryInfo);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(mockGitClientFactory.createApiClient(gitRepositoryInfo)).thenReturn(mockClient);
    try {
      when(mockClient.getRepositoryContributorsUsernames())
          .thenThrow(new IOException("Operation not available for SCM"));
    }
    catch (IOException e) {
      fail("Exception thrown when trying to mock getRepositoryContributorsUsernames call", e);
    }
  }

  private List<MembershipMapping> getDeveloperMembershipMappings() {
    try (TransactionContext tx = membershipMappingDAO.createTransactionContext()) {
      String developerRoleId = roleDAO.getByName("Developer").getId();
      List<MembershipMapping> membershipMappings =
          new ArrayList<>(membershipMappingDAO.getByRoleId(tx, developerRoleId));
      membershipMappings.sort(Comparator.comparing(MembershipMapping::getMemberName));
      return membershipMappings;
    }
  }

  private GitRepositoryInfo getGitRepositoryInfo() {
    return new GitRepositoryInfo(repoUrl, null, "user", "pass", SourceControlProvider.GITHUB, "main", true, true,
        true, true, false, null);
  }

  private GitRepositoryInfo getBitbucketRepositoryInfo() {
    return new GitRepositoryInfo(bitbucketRepoUrl, null, "user", "pass", SourceControlProvider.BITBUCKET, "main",
        true, true, true, true, false, null);
  }
}
