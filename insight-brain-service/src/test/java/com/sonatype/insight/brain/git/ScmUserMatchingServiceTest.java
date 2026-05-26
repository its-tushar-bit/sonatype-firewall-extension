/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.GITLOG_EMAIL;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.GITLOG_FULLNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.SCM_EMAIL;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.SCM_FULLNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.SCM_USERNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum.IQ_EMAIL;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum.IQ_FULLNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum.IQ_USERNAME;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.OWNER_ROLE_ID;
import static com.sonatype.insight.brain.utils.ScmUserMappingsHelper.getMappingForScmUserJsonStorage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMatchingResultDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.UserMapping;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.ContributorInfoProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.Contributor;
import com.sonatype.nexus.scm.api.model.ContributorPage;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ScmUserMatchingServiceTest
    extends AbstractComponentTest
{
  private Organization org;

  private Application app;

  private static final String REPO_URL = "https://github.com/sonatype/myAwesomeRepo";

  @Inject
  private ScmUserMatchingService scmUserMatchingService;

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private GitApiClient gitApiClient;

  @Mock
  private ContributorInfoProvider contributorInfoProvider;

  @Before
  public void setup() throws Exception {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());

    tempEntity.newSourceControl(org.getParentOrganizationId(), null, "token", SourceControlProvider.GITHUB);
  }

  // === BEGIN - Test all 15 possible From-To pairing combinations ===
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromScmUserNameToIqUserName() throws IOException {
    final var givenMapping = new UserMapping(SCM_USERNAME, IQ_USERNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("user1");
    tempEntity.newUser("user4");
    tempEntity.newUser("some-other-iq-user-not-found-on-github");

    final Set<String> givenGithubUsers = Sets.newHashSet("user1", "user2", "user3", "user4");
    mockGithubClientAndScmUtils(givenGithubUsers);

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("user1", "user4")));

    // should only fetch contributor usernames for this mapping
    verify(gitApiClient).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider, never()).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();
    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "user1", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "user4", DEVELOPER_ROLE_ID, app.getId());
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromScmEmailToIqEmail() throws IOException {

    final var givenMapping = new UserMapping(SCM_EMAIL, IQ_EMAIL);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-j-smith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");

    final var contributor1 = new Contributor(
        "Tod Walters",
        "twalters-commit@example.com",
        "twalters",
        "twalters@example.com",
        "Tod D. Walters");

    final var contributor2 = new Contributor(
        "Sam Jenkins",
        "sjenkins-commit@example.com",
        "sjenkins1",
        "sjenkins@example.com",
        "Sam B. Jenkins");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-sjenkins", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-sjenkins", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromGitLogEmailToIqEmail() throws IOException {
    final var givenMapping = new UserMapping(GITLOG_EMAIL, IQ_EMAIL);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");

    final var contributor1 = new Contributor(
        "Tod D Walters",
        "twalters@example.com",
        "twalters",
        "twalters-commit33@example.com",
        "Tod Walters");

    final var contributor2 = new Contributor(
        "Jim A Smith",
        "jim.smith@example.com",
        "sjenkins1",
        "jim.smith@example.com",
        "Jim Smith");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams44@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-jsmith", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromScmFullNameToIqFullName() throws IOException {
    final var givenMapping = new UserMapping(SCM_FULLNAME, IQ_FULLNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters11@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith1@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins22@example.com");

    final var contributor1 = new Contributor(
        "Tod D Walters",
        "twalters-commit@example.com",
        "twalters",
        "twalters@example.com",
        "Tod Walters");

    final var contributor2 = new Contributor(
        "Jim A Smith",
        "sjenkins-commit@example.com",
        "sjenkins1",
        "sjenkins@example.com",
        "Jim Smith");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-jsmith", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromGitLogFullNameToIqFullName() throws IOException {
    final var givenMapping = new UserMapping(GITLOG_FULLNAME, IQ_FULLNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");

    final var contributor1 = new Contributor(
        "Tod Walters",
        "twalters-commit33@example.com",
        "twalters",
        "twalters-commit33@example.com",
        "Tod Walters");

    final var contributor2 = new Contributor(
        "Jim Smith",
        "jim.smith32@example.com",
        "sjenkins1",
        "jim.smith@example.com",
        "Jim Smith");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams44@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-jsmith", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  // This is a less likely but possible mapping
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromGitLogFullNameToIqUserName() throws IOException {
    final var givenMapping = new UserMapping(GITLOG_FULLNAME, IQ_USERNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");

    final var contributor1 = new Contributor(
        // we are returning a username from the gitLogFullName
        // this is not as unlikely as some other scenarios as we are getting full name from the name used
        // supplied by the git client on comments, this couuld be a full name for a user name
        "iq-twalters",
        "twalters-commit33@example.com",
        "twalters",
        "twalters-commit33@example.com",
        "Tod Walters");

    final var contributor2 = new Contributor(
        "iq-jsmith",
        "jim.smith@example.com",
        "sjenkins1",
        "jim.smith@example.com",
        "Jim Smith");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams44@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-jsmith", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  // This is another, less likely, but definitely possible scenario for matching as using e-mail for authorName
  // could be something someone would do, though probably not since there is also a configurable git e-mail
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromGitLogFullNameToIqEmail() throws IOException {
    final var givenMapping = new UserMapping(GITLOG_FULLNAME, IQ_EMAIL);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");

    final var contributor1 = new Contributor(
        // returning e-mail in the author name slot
        "twalters@example.com",
        "twalters-commit33@example.com",
        "twalters",
        "twalters-commit33@example.com",
        "Tod Walters");

    final var contributor2 = new Contributor(
        "jim.smith@example.com",
        "jim.smith32@example.com",
        "sjenkins1",
        "jim.smith@example.com",
        "Jim Smith");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams44@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-jsmith", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  // unlikely real world mapping - but supported - so testing
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromScmUserNameToIqEmail() throws IOException {
    final var givenMapping = new UserMapping(SCM_USERNAME, IQ_EMAIL);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("jdoe", "John", "Doe", "jdoe29@example.com");
    tempEntity.newUser("j-smith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("ccarlyle42", "Carry", "Carlyle", "c-carlyle@example.com");

    // this is the part that's unrealistic. We are mocking the github service to return e-mails for scm user names.
    // in practice this should not happen, but doing so will let us test the mapping
    mockGithubClientAndScmUtils(
        Sets.newHashSet("jim.smith@example.com", "user2", "c-carlyle@example.com", "user4"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("j-smith", "ccarlyle42")));

    // should only fetch contributor usernames for this mapping
    verify(gitApiClient).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider, never()).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "ccarlyle42", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "j-smith", DEVELOPER_ROLE_ID, app.getId());
  }

  // unlikely real world mapping - but supported - so testing
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromScmUserNameToIqFullName() throws IOException {
    final var givenMapping = new UserMapping(SCM_USERNAME, IQ_FULLNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    // Avoid colliding with AbstractComponentTest's default test user, which is also "John Doe".
    tempEntity.newUser("jdoe", "John", "Doe-SCM", "jdoe29@example.com");
    tempEntity.newUser("j-smith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("ccarlyle42", "Carry", "Carlyle", "c-carlyle@example.com");

    // this is the part that's unrealistic. We are mocking the github service to return full names for scm user names.
    // in practice this should not happen, but doing so will let us test the mapping
    mockGithubClientAndScmUtils(Sets.newHashSet("John Doe-SCM", "Carry Carlyle", "some-user", "user4"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("jdoe", "ccarlyle42")));

    // should only fetch contributor usernames for this mapping
    verify(gitApiClient).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider, never()).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "ccarlyle42", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "jdoe", DEVELOPER_ROLE_ID, app.getId());
  }

  // unlikely real world mapping - but supported - so testing
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromScmEmailToIqUserName() throws IOException {
    final var givenMapping = new UserMapping(SCM_EMAIL, IQ_USERNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-j-smith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");

    final var contributor1 = new Contributor(
        "Tod Walters",
        "twalters@example.com",
        "twalters",
        // scmEmail would never return a username like this, but it's the only to force a match for testing
        "iq-twalters",
        "Tod D. Walters");

    final var contributor2 = new Contributor(
        "Sam Jenkins",
        "sjenkins@example.com",
        "sjenkins1",
        "iq-sjenkins",
        "Sam B. Jenkins");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams@example.com",
        "awilliams1",
        "awilliams@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-sjenkins", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-sjenkins", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  // unlikely real world mapping - but supported - so testing
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromScmFullNameToIqEmail() throws IOException {
    final var givenMapping = new UserMapping(SCM_FULLNAME, IQ_EMAIL);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-j-smith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");

    final var contributor1 = new Contributor(
        "Tod Walters",
        "twalters-commit@example.com",
        "twalters",
        "twalters@example.com",
        // we are returning an e-mail from the scmName to artificially test this scenario, in the real world
        // this strategy would likely simply not return any matches
        "sjenkins@example.com");

    final var contributor2 = new Contributor(
        "Sam Jenkins",
        "sjenkins-commit@example.com",
        "sjenkins1",
        "sjenkins@example.com",
        "twalters@example.com");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-sjenkins", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-sjenkins", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  // unlikely real world mapping - but supported - so testing
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromScmEmailToIqFullName() throws IOException {
    final var givenMapping = new UserMapping(SCM_EMAIL, IQ_FULLNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-j-smith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");

    final var contributor1 = new Contributor(
        "Tod Walters",
        "twalters-commit@example.com",
        "twalters",
        // we are returning a full name from the scmEmail to artificially test this scenario, in the real world
        // this strategy would likely simply not return any matches
        "Tod Walters",
        "Any Name-1");

    final var contributor2 = new Contributor(
        "Sam Jenkins",
        "sjenkins-commit@example.com",
        "sjenkins1",
        "Jim Smith",
        "twalters@example.com");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-twalters", "iq-j-smith")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-j-smith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  // unlikely real world mapping - but supported - so testing
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromScmFullNameToIqUserName() throws IOException {
    final var givenMapping = new UserMapping(SCM_FULLNAME, IQ_USERNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters11@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith1@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins22@example.com");

    final var contributor1 = new Contributor(
        "Tod D Walters",
        "twalters-commit@example.com",
        "twalters",
        "twalters-commit@example.com",
        "iq-twalters");

    final var contributor2 = new Contributor(
        "Jim A Smith",
        "sjenkins-commit@example.com",
        "sjenkins1",
        "sjenkins-commit@example.com",
        "iq-jsmith");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-jsmith", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  // This is another unlikely pair
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromGitLogEmailToIqUserName() throws IOException {
    final var givenMapping = new UserMapping(GITLOG_EMAIL, IQ_USERNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters11@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith1@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins22@example.com");

    final var contributor1 = new Contributor(
        "Tod D Walters",
        // the commitLogEmail is being mocked to return a user name for test purposes
        "iq-twalters",
        "twalters",
        "twalters-commit@example.com",
        "Tod Walters");

    final var contributor2 = new Contributor(
        "Jim A Smith",
        "iq-jsmith",
        "sjenkins1",
        "sjenkins-commit@example.com",
        "Jim Smith");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-jsmith", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  // This is another unlikely pair
  @Test
  public void testAutomaticRoleAssignmentByMapping_MapsToCorrectResourcesWhenFromGitLogEmailToIqFullName() throws IOException {
    final var givenMapping = new UserMapping(GITLOG_EMAIL, IQ_FULLNAME);

    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");

    final var contributor1 = new Contributor(
        "Tod D Walters",
        // to support the test we will reuturn a full name from the commitAuthorEmail
        "Tod Walters",
        "twalters",
        "twalters-commit33@example.com",
        "Tod Walters");

    final var contributor2 = new Contributor(
        "Jim A Smith",
        "Jim Smith",
        "sjenkins1",
        "jim.smith@example.com",
        "Jim Smith");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams44@example.com",
        "Mandy Williams");

    mockCreateApiClientForGithub();
    mockContributorProvider(new ContributorPage(
        Sets.newHashSet(contributor1, contributor2, contributor3), false, "some-cursor-token"));

    final List<UserMapping> givenMappings = Lists.newArrayList(givenMapping);

    final var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        givenMapping, Sets.newHashSet("iq-jsmith", "iq-twalters")));

    // should only fetch scm contributor info from getContributorsFromGitLogs, getRepositoryContributorsUsernames does
    // not provide e-mails
    verify(gitApiClient, never()).getRepositoryContributorsUsernames();
    verify(contributorInfoProvider).getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any());

    // should correctly save the roles
    final var mappedMembers = getDeveloperMembershipMappings();

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }
  // === END - Test All 15 Possible From to Paring Combinations ===

  @Test
  public void testAutomaticRoleAssignmentByMapping_shouldReturnAfterFirstMatchFoundOnAnyStrategy() throws IOException {
    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith@example.com");
    tempEntity.newUser("iq-sjenkins", "Sam", "Jenkins", "sjenkins@example.com");
    tempEntity.newUser("iq-patty", "Patty", "Williams", "pwilliams@example.com");

    // matches two users for SCM_USERNAME, IQ_USERNAME
    mockGithubClientAndScmUtils(Sets.newHashSet("iq-twalters", "iq-jsmith"));

    final var contributor1 = new Contributor(
        "Tod Walters", // GITLOG_FULLNAME, IQ_FULLNAME
        "twalters-commit33@example.com",
        "twalters",
        "twalters-commit33@example.com",
        "Tod Walters");

    final var contributor2 = new Contributor(
        "Jim A Smith",
        "jim.smith@example.com", // GITLOG_EMAIL, IQ_EMAIL
        "sjenkins1",
        "jim.smith@example.com",
        "Jim Smith");

    final var contributor3 = new Contributor(
        "Amanda Williams",
        "awilliams-commit@example.com",
        "awilliams1",
        "awilliams44@example.com",
        "Mandy Williams");

    final var contributor4 = new Contributor(
        "Patty D. Smith",
        "pwilliams@example.com", // GITLOG_EMAIL, IQ_EMAIL
        "awilliams1",
        "pwilliams@example.com",
        "psmith");

    mockContributorProvider(
        new ContributorPage(Sets.newHashSet(contributor1, contributor2, contributor3, contributor4),
            false,
            "some-cursor-token"));

    var givenMappings = Lists.newArrayList(
        new UserMapping(SCM_USERNAME, IQ_FULLNAME), // should not match anything
        new UserMapping(SCM_USERNAME, IQ_USERNAME),
        new UserMapping(GITLOG_EMAIL, IQ_EMAIL),
        new UserMapping(GITLOG_FULLNAME, IQ_FULLNAME));

    var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        new UserMapping(SCM_USERNAME, IQ_USERNAME), Sets.newHashSet("iq-twalters", "iq-jsmith")));

    givenMappings = Lists.newArrayList(
        new UserMapping(SCM_USERNAME, IQ_FULLNAME), // should not match anything
        new UserMapping(GITLOG_EMAIL, IQ_EMAIL),
        new UserMapping(SCM_USERNAME, IQ_USERNAME),
        new UserMapping(GITLOG_FULLNAME, IQ_FULLNAME));

    results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        new UserMapping(GITLOG_EMAIL, IQ_EMAIL), Sets.newHashSet("iq-jsmith", "iq-patty")));

    givenMappings = Lists.newArrayList(
        new UserMapping(SCM_USERNAME, IQ_FULLNAME), // should not match anything
        new UserMapping(GITLOG_FULLNAME, IQ_FULLNAME),
        new UserMapping(GITLOG_EMAIL, IQ_EMAIL),
        new UserMapping(SCM_USERNAME, IQ_USERNAME));

    results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // returns the users along with a indicator of which matching was successful
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        new UserMapping(GITLOG_FULLNAME, IQ_FULLNAME), Sets.newHashSet("iq-twalters")));
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_shouldHandleInstancesWhereNoStrategiesMatch() {
    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith@example.com");

    // none of the users will match any of the strategies
    mockGithubClientAndScmUtils(Sets.newHashSet("bom-smith", "tim-roberts"));

    final var givenMappings = Lists.newArrayList(
        new UserMapping(SCM_USERNAME, IQ_FULLNAME),
        new UserMapping(SCM_USERNAME, IQ_USERNAME));

    var results = scmUserMatchingService
        .automaticRoleAssignmentByMapping(app.getPublicId(), new SCMUserMappingsDTO(null, givenMappings));

    // should return a null entry for successfulMapping and an empty hash set
    assertThat(results).isEqualTo(new SCMUserMatchingResultDTO(
        null, Sets.newHashSet()));
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_shouldUseARoleOverrideWhenProvided() {
    final var givenRoleName = "Owner";
    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith@example.com");

    mockGithubClientAndScmUtils(Sets.newHashSet("iq-twalters", "iq-jsmith"));

    scmUserMatchingService.automaticRoleAssignmentByMapping(
        app.getPublicId(),
        new SCMUserMappingsDTO(givenRoleName, Lists.newArrayList(new UserMapping(SCM_USERNAME, IQ_USERNAME))));

    // should correctly save the roles
    final var mappedMembers = tempEntity.getMembershipMappings("Owner");

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", OWNER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", OWNER_ROLE_ID, app.getId());
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_usesPreConfiguredIfNoneProvidedToMethod() {
    // === Given ===
    tempEntity.newSourceControl(app.getId(), REPO_URL);
    tempEntity.newUser("iq-twalters", "Tod", "Walters", "twalters@example.com");
    tempEntity.newUser("iq-jsmith", "Jim", "Smith", "jim.smith@example.com");

    mockGithubClientAndScmUtils(Sets.newHashSet("iq-twalters", "iq-jsmith"));

    tempEntity.createScmUserMappings(
        DEVELOPER_ROLE_ID,
        org.getId(),
        Lists.newArrayList(getMappingForScmUserJsonStorage(SCM_USERNAME.name(), IQ_USERNAME.name())));

    // === Then ===
    final var result = scmUserMatchingService.automaticRoleAssignmentByMapping(app.getPublicId(), null);

    assertThat(result).isEqualTo(
        new SCMUserMatchingResultDTO(
            new UserMapping(SCM_USERNAME, IQ_USERNAME),
            Sets.newHashSet("iq-twalters", "iq-jsmith")));

    // should correctly save the roles
    final var mappedMembers = tempEntity.getMembershipMappings("Developer");

    assertThat(mappedMembers.size()).isEqualTo(2);
    assertMembershipEqual(mappedMembers.get(0), "iq-jsmith", DEVELOPER_ROLE_ID, app.getId());
    assertMembershipEqual(mappedMembers.get(1), "iq-twalters", DEVELOPER_ROLE_ID, app.getId());
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_throwsExceptionGivenNoMappingAndNonePreConfigured() {
    final var result = assertThrows(BadRequestException.class,
        () -> scmUserMatchingService.automaticRoleAssignmentByMapping(
            app.getPublicId(),
            null));

    assertThat(result.getMessage())
        .isEqualTo("An SCMUserMappingsDTO must be provided either with the request or at the organization level");
  }

  private void mockGithubClientAndScmUtils(Set<String> githubUsers) {
    final GitRepositoryInfo gitRepositoryInfo = mockCreateApiClientForGithub();

    when(mockGitClientFactory.createApiClient(gitRepositoryInfo)).thenReturn(gitApiClient);

    try {
      when(gitApiClient.getRepositoryContributorsUsernames()).thenReturn(githubUsers);
    }
    catch (IOException e) {
      fail("Exception thrown when trying to mock getRepositoryContributorsUsernames call", e);
    }
  }

  private GitRepositoryInfo mockCreateApiClientForGithub() {
    final GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo();
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(app.getId())).thenReturn(gitRepositoryInfo);

    return gitRepositoryInfo;
  }

  private void mockContributorProvider(
      final ContributorPage contributorPage) throws IOException
  {
    when(contributorInfoProvider.getContributorsFromGitLogs(anyString(), anyString(), anyInt(), any()))
        .thenReturn(contributorPage);

    when(mockGitClientFactory.createContributorInfoProvider(any())).thenReturn(contributorInfoProvider);
  }

  private List<MembershipMapping> getDeveloperMembershipMappings() {
    return tempEntity.getMembershipMappings(RoleDAO.DEVELOPER);
  }

  private GitRepositoryInfo getGitRepositoryInfo() {
    return new GitRepositoryInfo(REPO_URL, null, "user", "pass", SourceControlProvider.GITHUB, "main", true, true,
        true, true, true, true, false, null);
  }

  private void assertMembershipEqual(
      final MembershipMapping actualMembershipMapping,
      final String expectedMemberName,
      final String expectedRoleId,
      final String expectedContextId)
  {
    assertThat(actualMembershipMapping.getMemberName()).isEqualTo(expectedMemberName);
    assertThat(actualMembershipMapping.getRoleId()).isEqualTo(expectedRoleId);
    assertThat(actualMembershipMapping.getContextId()).isEqualTo(expectedContextId);
  }
}
