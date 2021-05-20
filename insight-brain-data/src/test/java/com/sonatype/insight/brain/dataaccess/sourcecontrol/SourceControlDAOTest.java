/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO.PULL_REQUEST_POLLING_INITIAL_OFFSET_MS;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SourceControlDAOTest
    extends AbstractDbDAOTest
{
  private static final String VALID_URL = "https://example.com/organization/Project";

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private Application app;

  private Organization org;

  @Override
  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
  }

  @After
  public void cleanup() {
    sourceControlDAO.getAll().stream().forEach(sourceControlDAO::delete);
  }

  @Test
  public void testInitializePullRequestPollTimes_appWithOlderPolicyEvaluationAndNeedingPollTime() {
    // given: several policy evaluations at different times and a related source control without a poll time;
    //        the oldest policy eval is older than the polling offset
    final long pollingOffset = PULL_REQUEST_POLLING_INITIAL_OFFSET_MS / (1000L * 60 * 60);
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(pollingOffset + 2));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId2", false, false, false,
        toDate(now.minusHours(pollingOffset - 2)), "commitHash1234");
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId", false, false, false, scanTime,
        "commitHash123");
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId3", false, false, false,
        toDate(now.minusHours(pollingOffset - 4)), "commitHash1235");

    // when: fetch source control
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: poll time is not set
    assertThat(sourceControl.getPullRequestPollTime()).isNull();

    // when: update poll times and fetch app source control
    sourceControlDAO.initializePullRequestPollTimes();
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: source control poll time for app was updated to the earliest policy eval time
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(scanTime);
  }

  @Test
  public void testInitializePullRequestPollTimes_appWithNewerPolicyEvaluationAndNeedingPollTime() {
    // given: several policy evaluations at different times and a related source control without a poll time;
    //        the oldest policy eval is newer than the polling offset
    final long pollingOffset = PULL_REQUEST_POLLING_INITIAL_OFFSET_MS / (1000L * 60 * 60);
    LocalDateTime now = LocalDateTime.now();
    final Date defaultPollingTime = toDate(now.minusHours(pollingOffset));
    Date scanTime = toDate(now.minusHours(pollingOffset - 2));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId2", false, false, false,
        toDate(now.minusHours(pollingOffset - 4)), "commitHash1234");
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId", false, false, false, scanTime,
        "commitHash123");
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId3", false, false, false,
        toDate(now.minusHours(pollingOffset - 6)), "commitHash1235");

    // when: fetch source control
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: poll time is not set
    assertThat(sourceControl.getPullRequestPollTime()).isNull();

    // when: update poll times and fetch app source control
    sourceControlDAO.initializePullRequestPollTimes();
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: source control poll time for app was updated to the default offset time
    assertThat(sourceControl.getPullRequestPollTime()).isBefore(scanTime);
    assertThat(sourceControl.getPullRequestPollTime()).isAfterOrEqualTo(defaultPollingTime);
  }

  @Test
  public void testInitializePullRequestPollTimes_appWithPolicyEvaluationAndAlreadyHasPollTime() {
    // given: policy eval and app source control with poll time
    Date scanTime = toDate(LocalDateTime.now().minusDays(3));
    Date startTime = new Date();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", startTime);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId", false, false, false, scanTime,
        "commitHash123");

    // when: fetch source control
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: poll time is set
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(startTime);

    // when: update poll times and fetch app source control
    sourceControlDAO.initializePullRequestPollTimes();
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: source control poll time for app was not updated
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(startTime);
  }

  @Test
  public void testInitializePullRequestPollTimes_appWithRepoUrlAndNeedsPollTime_GitLab() {
    // given: app source control without poll time and no related policy evals
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);

    // when: fetch source control
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: poll time missing
    assertThat(sourceControl.getPullRequestPollTime()).isNull();

    // when: update poll times and refetch
    Date expectedPullRequestPollTime =
        new Date(System.currentTimeMillis() - PULL_REQUEST_POLLING_INITIAL_OFFSET_MS);
    Date now = new Date();
    sourceControlDAO.initializePullRequestPollTimes();
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: new poll time was assigned
    assertThat(sourceControl.getPullRequestPollTime()).isNotNull();
    assertThat(sourceControl.getPullRequestPollTime()).isAfterOrEqualTo(expectedPullRequestPollTime);
    assertThat(sourceControl.getPullRequestPollTime()).isBefore(now);
  }

  @Test
  public void testInitializePullRequestPollTimes_appWithRepoUrlAndNeedsPollTime_GitHub() {
    // given: app source control without poll time and no related policy evals
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);

    // when: fetch source control
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: poll time missing
    assertThat(sourceControl.getPullRequestPollTime()).isNull();

    // when: update poll times and refetch
    Date expectedPullRequestPollTime = new Date();
    sourceControlDAO.initializePullRequestPollTimes();
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: new poll time was assigned
    assertThat(sourceControl.getPullRequestPollTime()).isNotNull();
    assertThat(sourceControl.getPullRequestPollTime()).isAfterOrEqualTo(expectedPullRequestPollTime);
  }

  @Test
  public void testInitializePullRequestPollTimes_sourceControlWithoutRepoUrlAndWithPollTimeSet() {
    // given: source control with null repo url and with poll time set
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(org.getId(), null, new Date());

    // when: fetch source control for app
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(org.getId());

    // then: poll time not null
    assertThat(sourceControl.getPullRequestPollTime()).isNotNull();

    // when: update poll times and re-fetch
    sourceControlDAO.initializePullRequestPollTimes();
    sourceControl = sourceControlDAO.getByOwnerId(org.getId());

    // then: source control poll time set to null
    assertThat(sourceControl.getPullRequestPollTime()).isNull();
  }

  @Test
  public void testGetByRepositoryOwnerAndName() {
    // given: a source control entry with the desired repo owner and name
    String repoOwnerAndName = "testOrg/repoName";
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://domain.com/" + repoOwnerAndName, "", null);

    // when: find the source control entry by owner and name string
    List<SourceControl> sourceControlList = sourceControlDAO.getByRepositoryOwnerAndName(repoOwnerAndName);

    // then: we found it
    assertThat(sourceControlList).isNotNull();
    assertThat(sourceControlList.size()).isEqualTo(1);
    assertThat(sourceControlList.get(0).getOwnerId()).isEqualTo(app.getId());

    // when: add a 2nd source control entry for same repo and search again
    Application app2 = tempEntity.newApplication(app.getOrganizationId());
    tempEntity.newSourceControl(app2.getId(), "http://domain.com/" + repoOwnerAndName, null);
    sourceControlList = sourceControlDAO.getByRepositoryOwnerAndName(repoOwnerAndName);

    // then: we have 2 now
    assertThat(sourceControlList.size()).isEqualTo(2);
    assertThat(sourceControlList).extracting(SourceControl::getOwnerId)
        .containsExactlyInAnyOrder(app.getId(), app2.getId());
  }

  @Test
  public void testGetNextRepositoryToPoll() {
    // given: several source control entries with different pull request poll times
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    LocalDateTime dateTime = LocalDateTime.now();
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo1", toDate(dateTime));

    Application app2 = tempEntity.newApplication("app2", org.getId());
    SourceControl sourceControl2 = tempEntity.newSourceControl(app2.getId(), "http://a.com/org/repo2",
        toDate(dateTime.minusDays(1)));

    Application app3 = tempEntity.newApplication("app3", org.getId());
    SourceControl sourceControl3 = tempEntity.newSourceControl(app3.getId(), "http://a.com/org/repo3",
        toDate(dateTime.minusDays(2)));

    // when: get source control for next repo to poll
    SourceControl sourceControl = sourceControlDAO.getNextRepositoryToPoll();

    // then: app3 has oldest poll time
    assertThat(sourceControl.getOwnerId()).isEqualTo(app3.getId());

    // when: update app3 source control poll time to have current time
    sourceControl3.setPullRequestPollTime(new Date());
    sourceControlDAO.update(sourceControl3);
    sourceControl = sourceControlDAO.getNextRepositoryToPoll();

    // then: app2 source control now has oldest poll time
    assertThat(sourceControl.getOwnerId()).isEqualTo(app2.getId());

    // when: clear app2 poll time and lookup next repo to poll
    sourceControl2.setPullRequestPollTime(null);
    sourceControlDAO.update(sourceControl2);
    sourceControl = sourceControlDAO.getNextRepositoryToPoll();

    // then: app 1 source control has oldest poll time now
    assertThat(sourceControl.getOwnerId()).isEqualTo(app.getId());
  }

  @Test
  public void testGetNextRepositoryToPoll_future() {
    // given: source control entries with pull request poll time in future
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    LocalDateTime dateTime = LocalDateTime.now();
    SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo1", toDate(dateTime.plusDays(1)));

    // when: get source control for next repo to poll
    SourceControl sourceControl = sourceControlDAO.getNextRepositoryToPoll();

    // then: nothing should be returned
    assertThat(sourceControl).isNull();

    // when: update app source control poll time to have past time
    appSourceControl.setPullRequestPollTime(toDate(dateTime.minusDays(1)));
    sourceControlDAO.update(appSourceControl);
    sourceControl = sourceControlDAO.getNextRepositoryToPoll();

    // then: app source control is returned
    assertThat(sourceControl.getOwnerId()).isEqualTo(app.getId());
  }

  @Test
  public void testInsert_MissingOwnerId() {
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(new SourceControl.Builder().build());
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl owner id is required");
  }

  @Test
  public void testInsert_RepositoryUrlForOrganization() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(org.getId()).setRepositoryUrl(VALID_URL).setToken("token").build();
    assertThatThrownBy(() ->
        sourceControlDAO.insert(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage(
        "SourceControl repositoryUrl is not allowed for organization");
  }

  @Test
  public void testInsert_RootOrgDefaults() {
    String baseBranch = "development";

    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(ROOT_ORGANIZATION_ID)
        .setProvider(GITLAB)
        .setBaseBranch(baseBranch)
        .setEnablePullRequests(null)
        .setEnableStatusChecks(null)
        .build();

    sourceControlDAO.insert(sourceControl);

    SourceControl persistedSourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(persistedSourceControl.getEnablePullRequests()).isTrue();
    assertThat(persistedSourceControl.getEnableStatusChecks()).isTrue();
  }

  @Test
  public void testInsert_RootOrgMissingDefaultBranch() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(ROOT_ORGANIZATION_ID)
        .setProvider(GITLAB)
        .build();

    assertThatThrownBy(() -> sourceControlDAO.insert(sourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("SourceControl default branch is required for the root organization");
  }

  @Test
  public void testInsert_MissingRepositoryUrlForApplication() {
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setToken("token").build();
    assertThatThrownBy(() ->
        sourceControlDAO.insert(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage(
        "SourceControl repositoryUrl is required for application");
  }

  @Test
  public void testInsert_InvalidUrl() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl("https://not valid").setToken("token")
            .build();
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("repositoryUrl is invalid");
  }

  @Test
  public void testInsert_CannotValidateUrl() {
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl("https://not valid").build();
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("Cannot validate SourceControl repositoryUrl");
  }

  @Test
  public void testInsert_AppPublicIdDoesNotExist() {
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId("baz").setRepositoryUrl(VALID_URL).setToken("bar")
            .build();
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl ownerId 'baz' cannot be found");
  }

  @Test
  public void testInsert_DuplicateRepositoryUrlAllowed() {
    createRootOrgWithGitHubProvider();
    Application baz = tempEntity.newApplicationWithParent("baz");
    tempEntity.newSourceControl(baz.getId(), VALID_URL, "bar", null);
    sourceControlDAO
        .insert(new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("bar")
            .build());
  }

  @Test
  public void testUpdate_MissingOwnerId() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", null);
    sourceControl.setOwnerId(null);
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl owner id is required");
  }

  @Test
  public void testUpdate_MissingRepositoryUrlForApplication() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "bar", null);
    sourceControl.setRepositoryUrl(null);
    assertThatThrownBy(() ->
        sourceControlDAO.update(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage("SourceControl repositoryUrl is required for application");
  }

  @Test
  public void testUpdate_RepositoryUrlForOrganization() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(
        org.getId(), null, "bar", null);
    sourceControl.setRepositoryUrl(VALID_URL);
    assertThatThrownBy(() ->
        sourceControlDAO.update(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage("SourceControl repositoryUrl is not allowed for organization");
  }

  @Test
  public void testUpdate_InvalidUrl() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", null);
    sourceControl.setRepositoryUrl("https://not valid");
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("repositoryUrl is invalid");
  }

  @Test
  public void testUpdate_DuplicateRepositoryUrlAllowed() {
    createRootOrgWithGitHubProvider();
    Application baz = tempEntity.newApplicationWithParent();
    Application foo = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(baz.getId(), VALID_URL, "bar", null);
    SourceControl sourceControl =
        tempEntity.newSourceControl(foo.getId(), VALID_URL + ".1", "bar", null);
    sourceControl.setRepositoryUrl(VALID_URL);
    sourceControlDAO.update(sourceControl);
  }

  @Test
  public void testCRUD_Application() {
    assertThat(VALID_URL.toLowerCase(Locale.ENGLISH)).isNotEqualTo(VALID_URL);

    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL)
            .setToken("bar").setBaseBranch("base/branch").setEnablePullRequests(true)
            .setEnableStatusChecks(true).build();

    assertThat(sourceControl.getId()).isNull();
    sourceControlDAO.insert(sourceControl);
    assertThat(sourceControl.getId()).isNotNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getOwnerId()).isEqualTo(app.getId());
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(VALID_URL.toLowerCase(Locale.ENGLISH));
    assertThat(sourceControl.getUsername()).isNull();
    assertThat(sourceControl.getToken()).isEqualTo("bar");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("base/branch");
    assertThat(sourceControl.getEnablePullRequests()).isTrue();
    assertThat(sourceControl.getEnableStatusChecks()).isTrue();

    sourceControl.setToken("baz");
    sourceControl.setBaseBranch("another");
    sourceControl.setEnablePullRequests(false);
    sourceControl.setEnableStatusChecks(false);
    sourceControlDAO.update(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getUsername()).isNull();
    assertThat(sourceControl.getToken()).isEqualTo("baz");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("another");
    assertThat(sourceControl.getEnablePullRequests()).isFalse();
    assertThat(sourceControl.getEnableStatusChecks()).isFalse();

    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testCRUD_Organization() {
    createRootOrgWithGitHubProvider();

    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(org.getId()).setToken("bar")
            .setEnablePullRequests(true).setEnableStatusChecks(true)
            .setBaseBranch("base/branch").build();

    assertThat(sourceControl.getId()).isNull();
    sourceControlDAO.insert(sourceControl);
    assertThat(sourceControl.getId()).isNotNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getOwnerId()).isEqualTo(org.getId());
    assertThat(sourceControl.getUsername()).isNull();
    assertThat(sourceControl.getToken()).isEqualTo("bar");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("base/branch");
    assertThat(sourceControl.getEnablePullRequests()).isTrue();
    assertThat(sourceControl.getEnableStatusChecks()).isTrue();

    sourceControl.setToken("baz");
    sourceControl.setBaseBranch("another");
    sourceControl.setEnablePullRequests(false);
    sourceControl.setEnableStatusChecks(false);
    sourceControlDAO.update(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getUsername()).isNull();
    assertThat(sourceControl.getToken()).isEqualTo("baz");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("another");
    assertThat(sourceControl.getEnablePullRequests()).isFalse();
    assertThat(sourceControl.getEnableStatusChecks()).isFalse();

    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testInsert_PullRequestConfigsCanBeNull() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("bar")
            .build();

    assertThat(sourceControl.getId()).isNull();
    assertThat(sourceControl.getBaseBranch()).isNull();
    assertThat(sourceControl.getEnablePullRequests()).isNull();
    assertThat(sourceControl.getEnableStatusChecks()).isNull();

    sourceControlDAO.insert(sourceControl);

    assertThat(sourceControl.getId()).isNotNull();
    assertThat(sourceControl.getBaseBranch()).isNull();
    assertThat(sourceControl.getEnablePullRequests()).isNull();
    assertThat(sourceControl.getEnableStatusChecks()).isNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getBaseBranch()).isNull();
    assertThat(sourceControl.getEnablePullRequests()).isNull();
    assertThat(sourceControl.getEnableStatusChecks()).isNull();
  }

  @Test
  public void testGetAll() {
    createRootOrgWithGitHubProvider();
    assertThat(sourceControlDAO.getAll()).hasSize(1);
    Application app2 = tempEntity.newApplicationWithParent("bar");
    tempEntity.newSourceControl(app.getId(), VALID_URL, "token", null);
    tempEntity.newSourceControl(app2.getId(), VALID_URL, "token", null);

    List<SourceControl> scms = sourceControlDAO.getAll();
    assertThat(scms).hasSize(3);
    Stream<String> appIds = scms.stream().map(SourceControl::getOwnerId);
    assertThat(appIds.collect(Collectors.toList()).containsAll(Arrays.asList(app.getId(), "bar")));
  }

  @Test
  public void testInsert_ProviderFromOrganization() {
    createRootOrgWithGitHubProvider();
    tempEntity.newSourceControl(app.getOrganizationId(), null, "token", null);
    sourceControlDAO.insert(
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).build());
  }

  @Test
  public void testInsert_ProviderFromRootOrganization() {
    tempEntity.newSourceControl(org.getParentOrganizationId(), null, "token", SourceControlProvider.GITHUB);
    sourceControlDAO.insert(
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).build());
  }

  @Test
  public void testInsert_ProviderNotAvailable() {
    assertThatThrownBy(
        () -> sourceControlDAO.insert(
            new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).build()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Cannot validate SourceControl repositoryUrl");
  }

  @Test
  public void testUpdate_ProviderFromRootOrganization() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("TOKEN").build();
    sourceControlDAO.insert(sourceControl);
    sourceControl.setProvider(null);
    sourceControl.setToken(null);
    sourceControlDAO.update(sourceControl);
  }

  @Test
  public void testUpdate_ProviderNotAvailable() {
    SourceControl root = createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("TOKEN")
            .build();
    sourceControlDAO.insert(sourceControl);
    sourceControl.setProvider(null);
    sourceControl.setToken(null);
    sourceControlDAO.delete(root);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Cannot validate SourceControl repositoryUrl");
  }

  @Test
  public void testGetByApplication() {
    createRootOrgWithGitHubProvider();
    // create a few sample entries
    SourceControl scApp1 = buildAppSourceControl(app.getId(), 1, null);
    sourceControlDAO.insert(scApp1);

    SourceControl scApp2 = buildAppSourceControlAndApp(org, 2, null);
    sourceControlDAO.insert(scApp2);

    SourceControl scApp3 = buildAppSourceControlAndApp(org, 3, null);
    sourceControlDAO.insert(scApp3);

    // create source control entries for organizations which will not have repository URLs set
    sourceControlDAO.insert(buildOrgSourceControl(org.getId(), null));

    assertThat(sourceControlDAO.getByApplication())
        .hasSize(3).extracting(SourceControl::getId)
        .containsExactlyInAnyOrder(scApp1.getId(), scApp2.getId(), scApp3.getId());
  }

  @Test
  public void testGetApplicationsWithPullReqsEnabled_enabledAtRoot() {
    // create SCM entry at root, with PR enabled flag set
    SourceControl rootSourceControl = buildOrgSourceControl(Organization.ROOT_ORGANIZATION_ID, true);
    rootSourceControl.setProvider(SourceControlProvider.GITHUB);
    rootSourceControl.setBaseBranch("master");
    sourceControlDAO.insert(rootSourceControl);

    // create a few child orgs of root
    Organization orgNullPrs = tempEntity.newOrganization();
    Organization orgNoPrs = tempEntity.newOrganization();

    sourceControlDAO.insert(buildOrgSourceControl(orgNullPrs.getId(), null));

    sourceControlDAO.insert(buildOrgSourceControl(orgNoPrs.getId(), false));

    // enabled at app level, disabled at org level => enabled
    SourceControl scEnabledAtAppDisabledAtOrg = buildAppSourceControlAndApp(orgNoPrs, 2, true);
    sourceControlDAO.insert(scEnabledAtAppDisabledAtOrg);

    // default at app level, default org level, enabled root => enabled
    SourceControl scDefaultAppDefaultOrg = buildAppSourceControlAndApp(orgNullPrs, 3, null);
    sourceControlDAO.insert(scDefaultAppDefaultOrg);

    // default at app level, disabled org level => not enabled
    SourceControl scDefaultAppDisabledOrg = buildAppSourceControlAndApp(orgNoPrs, 4, false);
    sourceControlDAO.insert(scDefaultAppDisabledOrg);

    // create entries at the application level without the enable PR flag set. Use null & explicitly enable
    SourceControl scDefault = buildAppSourceControl(app.getId(), 1, null);
    sourceControlDAO.insert(scDefault);
    SourceControl scExplicitlyEnabled = buildAppSourceControlAndApp(org, 2, true);
    sourceControlDAO.insert(scExplicitlyEnabled);

    // create application entries that are explicitly disabled
    sourceControlDAO.insert(buildAppSourceControlAndApp(org, 3, false));
    sourceControlDAO.insert(buildAppSourceControlAndApp(org, 4, false));

    Collection<SourceControl> enabledApplications = sourceControlDAO.getApplicationsWithPullReqsEnabled();
    assertThat(enabledApplications).extracting(SourceControl::getId).containsExactlyInAnyOrder(
        scExplicitlyEnabled.getId(), scDefault.getId(), scEnabledAtAppDisabledAtOrg.getId(),
        scDefaultAppDefaultOrg.getId());
  }

  @Test
  public void testGetApplicationsWithPullReqsEnabled_enabledAtOrgAndApp() {
    createRootOrgWithGitHubProvider();
    // create SCM entries for organizations, with PR enabled flag set
    Organization orgNullPrs = tempEntity.newOrganization();
    Organization orgNoPrs = tempEntity.newOrganization();
    Organization orgEnabledPrs = tempEntity.newOrganization();

    sourceControlDAO.insert(buildOrgSourceControl(orgEnabledPrs.getId(), true));

    // null for enabled defaults to true
    sourceControlDAO.insert(buildOrgSourceControl(orgNullPrs.getId(), null));

    sourceControlDAO.insert(buildOrgSourceControl(orgNoPrs.getId(), false));

    // null at app level, enabled at org level => enabled
    SourceControl scEnabledAtOrg = buildAppSourceControlAndApp(orgEnabledPrs, 1, null);
    sourceControlDAO.insert(scEnabledAtOrg);

    // enabled at app level, disabled at org level => enabled
    SourceControl scEnabledAtAppDisabledAtOrg = buildAppSourceControlAndApp(orgNoPrs, 2, true);
    sourceControlDAO.insert(scEnabledAtAppDisabledAtOrg);

    // default at app level, default org level => not enabled
    SourceControl scDefaultAppDefaultOrg = buildAppSourceControlAndApp(orgNullPrs, 3, null);
    sourceControlDAO.insert(scDefaultAppDefaultOrg);

    // default at app level, disabled org level => not enabled
    SourceControl scDefaultAppDisabledOrg = buildAppSourceControlAndApp(orgNoPrs, 4, false);
    sourceControlDAO.insert(scDefaultAppDisabledOrg);

    Collection<SourceControl> enabledApplications = sourceControlDAO.getApplicationsWithPullReqsEnabled();
    assertThat(enabledApplications).extracting(SourceControl::getId)
        .hasSize(3)
        .containsExactlyInAnyOrder(scEnabledAtAppDisabledAtOrg.getId(), scEnabledAtOrg.getId(),
            scDefaultAppDefaultOrg.getId());
  }

  private SourceControl buildAppSourceControlAndApp(
      Organization organization,
      int appNumber,
      Boolean enablePullRequests)
  {
    return buildAppSourceControl(tempEntity.newApplication(organization.getId()).getId(), appNumber,
        enablePullRequests);
  }

  private SourceControl buildAppSourceControl(String ownerId, int appNumber, Boolean enablePullRequests) {
    return new SourceControl.Builder()
        .setOwnerId(ownerId)
        .setRepositoryUrl("http://localhost/owner/app" + appNumber)
        .setEnablePullRequests(enablePullRequests)
        .build();
  }

  private SourceControl buildOrgSourceControl(String ownerId, Boolean enablePullRequests) {
    return new SourceControl.Builder()
        .setOwnerId(ownerId)
        .setToken("token")
        .setEnablePullRequests(enablePullRequests)
        .build();
  }

  @Test
  public void testInsert_RootOrgWithoutProvider() {
    assertThatThrownBy(
        () -> sourceControlDAO.insert(new SourceControl.Builder().setOwnerId(ROOT_ORGANIZATION_ID).build()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider is required for the root organization");
  }

  @Test
  public void testUpdate_RootOrgDefaults() {
    String baseBranch = "development";

    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(ROOT_ORGANIZATION_ID)
        .setProvider(GITLAB)
        .setBaseBranch(baseBranch)
        .setEnablePullRequests(false)
        .setEnableStatusChecks(false)
        .build();

    sourceControlDAO.insert(sourceControl);

    SourceControl persistedSourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(persistedSourceControl.getEnablePullRequests()).isFalse();
    assertThat(persistedSourceControl.getEnableStatusChecks()).isFalse();

    persistedSourceControl.setEnablePullRequests(null);
    persistedSourceControl.setEnableStatusChecks(null);

    sourceControlDAO.update(persistedSourceControl);

    SourceControl updatedSourceControl = sourceControlDAO.getById(persistedSourceControl.getId());
    assertThat(updatedSourceControl.getEnablePullRequests()).isTrue();
    assertThat(updatedSourceControl.getEnableStatusChecks()).isTrue();
  }

  @Test
  public void testUpdate_RootOrgMissingDefaultBranch() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(ROOT_ORGANIZATION_ID)
        .setProvider(GITLAB)
        .setBaseBranch("master")
        .build();

    sourceControlDAO.insert(sourceControl);

    SourceControl persistedSourceControl = sourceControlDAO.getById(sourceControl.getId());
    persistedSourceControl.setBaseBranch(null);

    assertThatThrownBy(() -> sourceControlDAO.update(persistedSourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("SourceControl default branch is required for the root organization");
  }

  @Test
  public void testUpdate_RootOrgWithoutProvider() {
    SourceControl sourceControl = createRootOrgWithGitHubProvider();
    sourceControl.setProvider(null);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider is required for the root organization");
  }

  @Test
  public void testInsert_OrganizationWithProvider() {
    assertThatThrownBy(
        () -> sourceControlDAO.insert(
            new SourceControl.Builder().setOwnerId(org.getId()).setProvider(SourceControlProvider.GITHUB).build()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider can only be specified on the root organization");
  }

  @Test
  public void testUpdate_OrganizationWithProvider() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(org.getId(), null, "token", null);
    sourceControl.setProvider(SourceControlProvider.GITHUB);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider can only be specified on the root organization");
  }

  @Test
  public void testInsert_ApplicationWithProvider() {
    assertThatThrownBy(
        () -> sourceControlDAO.insert(
            new SourceControl.Builder().setOwnerId(app.getId()).setProvider(SourceControlProvider.GITHUB).build()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider can only be specified on the root organization");
  }

  @Test
  public void testUpdate_ApplicationWithProvider() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL, null, null);
    sourceControl.setProvider(SourceControlProvider.GITHUB);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider can only be specified on the root organization");
  }

  @Test
  public void testUpdatePollTimeAndErrorCounts() {
    createRootOrgWithGitHubProvider();
    SourceControl scApp1 = buildAppSourceControl(app.getId(), 1, true);
    sourceControlDAO.insert(scApp1);

    Date now = new Date();
    sourceControlDAO.updatePollTimeAndErrorCounts(scApp1.getId(), now, 5);

    scApp1 = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(scApp1.getPullRequestPollTime()).isEqualTo(now);
    assertThat(scApp1.getPullRequestErrorCount()).isEqualTo(5);
  }

  private SourceControl createRootOrgWithGitHubProvider() {
    return tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
  }

  private Date toDate(LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  @Test
  public void testGetApplicationSourceControlsByOrganizationWithRepositories() {
    // given a root org with github as a provider
    createRootOrgWithGitHubProvider();
    // and several apps with SC entries in the initial org
    Application app1a = tempEntity.newApplication("my-app-1", org.getId());
    SourceControl scApp1a = buildAppSourceControl(app1a.getId(), 1, null);
    scApp1a.setRepositoryUrl("https://myhost.com/org/app-1");
    sourceControlDAO.insert(scApp1a);

    Application app1b = tempEntity.newApplication("my-app-2", org.getId());
    SourceControl scApp1b = buildAppSourceControl(app1b.getId(), 2, null);
    scApp1b.setRepositoryUrl("https://myhost.com/org/app-2");
    sourceControlDAO.insert(scApp1b);

    // define a token, so should be excluded from search results
    Application app1c = tempEntity.newApplication("my-app-3", org.getId());
    SourceControl scApp1c = buildAppSourceControl(app1c.getId(), 3, null);
    scApp1c.setRepositoryUrl("https://myhost.com/org/app-3");
    scApp1c.setToken("sample-token");
    sourceControlDAO.insert(scApp1c);

    // and several SC entries for another org
    Organization org2 = tempEntity.newOrganization();
    Application app2a = tempEntity.newApplication("my-app-2-1", org2.getId());
    SourceControl scApp2a = buildAppSourceControl(app2a.getId(), 1, null);
    sourceControlDAO.insert(scApp2a);

    Application app2b = tempEntity.newApplication("my-app-2-2", org2.getId());
    SourceControl scApp2b = buildAppSourceControl(app2b.getId(), 2, null);
    sourceControlDAO.insert(scApp2b);

    // then we get the SC entries for the first org
    assertThat(sourceControlDAO.getApplicationSourceControlsByOrganizationWithRepositories(org.getId()))
        .extracting(SourceControl::getId)
        .containsExactlyInAnyOrder(scApp1a.getId(), scApp1b.getId());

    // and we get the SC entries for the second org
    assertThat(sourceControlDAO.getApplicationSourceControlsByOrganizationWithRepositories(org2.getId()))
        .extracting(SourceControl::getId)
        .containsExactlyInAnyOrder(scApp2a.getId(), scApp2b.getId());
  }

  @Test
  public void testGetApplicationSourceControlsWithRepositoriesAndDefaultToken() {
    // given a root org with github as a provider
    createRootOrgWithGitHubProvider();
    // and an app with a SC entry in the initial org
    Application app1a = tempEntity.newApplication("my-app-1", org.getId());
    SourceControl scApp1a = buildAppSourceControl(app1a.getId(), 1, null);
    scApp1a.setRepositoryUrl("https://myhost.com/org/app-1");
    sourceControlDAO.insert(scApp1a);

    // given an org with a custom token
    Organization orgCustom = tempEntity.newOrganization("custom");
    sourceControlDAO.insert(new SourceControl.Builder()
        .setOwnerId(orgCustom.getId())
        .setToken("token")
        .build()
    );

    // and given an app with a custom token
    sourceControlDAO.insert(new SourceControl.Builder()
        .setOwnerId(app.getId())
        .setRepositoryUrl("https://mhost.com/org/custom-token-app")
        .setToken("app-token")
        .build()
    );

    // given an app with a custom repo URL
    Application appCustom = tempEntity.newApplication(orgCustom.getId());
    SourceControl scAppCustom = new SourceControl.Builder()
        .setOwnerId(appCustom.getId())
        .setRepositoryUrl("http://example.com/owner/app")
        .build();
    sourceControlDAO.insert(scAppCustom);

    // when we get applications with only the default token
    List<SourceControl> appsWithDefaultTokens =
        sourceControlDAO.getApplicationSourceControlsWithRepositoriesAndDefaultToken();

    // then it doesn't contain the apps with custom tokens or with orgs that have custom tokens
    assertThat(appsWithDefaultTokens)
        .extracting(SourceControl::getId)
        .containsOnly(scApp1a.getId());
  }

  @Test
  public void testDelete_NullEntity() {
    // Should not throw an exception
    sourceControlDAO.delete(null);
  }

  @Test
  public void testDelete_CascadesToPullRequests() {
    // given a root org with github as a provider
    createRootOrgWithGitHubProvider();
    // And two source controls (for two apps) with the same repository URL
    Application app1 = tempEntity.newApplicationWithParent();
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL);
    SourceControl sourceControl1 = tempEntity.newSourceControl(app1.getId(), VALID_URL);
    tempEntity.newSourceControlPullRequest(VALID_URL, 1, "testCommitHash", "testBranch", new Date(), new Date(),
        new Date());

    // Then delete should not cascade to pull requests (because there are two source control records with the same
    // repository URL).
    sourceControlDAO.delete(sourceControl);
    assertThat(new SourceControlPullRequestDAO().getAll()).hasSize(1);

    // Then delete should cascade to pull requests (because there is only one source control record left with that
    // repository URL).
    sourceControlDAO.delete(sourceControl1);
    assertThat(new SourceControlPullRequestDAO().getAll()).hasSize(0);
  }

  @Test
  public void testUpdate_DeletesOrphanPullRequestsIfRepositoryUrlIsChanged() {
    // given a root org with github as a provider
    createRootOrgWithGitHubProvider();
    // And two source controls (for two apps) with the same repository URL
    Application app1 = tempEntity.newApplicationWithParent();
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL);
    SourceControl sourceControl1 = tempEntity.newSourceControl(app1.getId(), VALID_URL);
    tempEntity.newSourceControlPullRequest(VALID_URL, 1, "testCommitHash", "testBranch", new Date(), new Date(),
        new Date());

    // Then update should not delete pull requests (because there were two source control records with the same
    // repository URL).
    sourceControl.setRepositoryUrl(VALID_URL + "Updated");
    sourceControlDAO.update(sourceControl);
    assertThat(new SourceControlPullRequestDAO().getAll()).hasSize(1);

    // Then update should delete pull requests (because there is only one source control record left with that
    // repository URL).
    sourceControl1.setRepositoryUrl(VALID_URL + "Updated1");
    sourceControlDAO.update(sourceControl1);
    assertThat(new SourceControlPullRequestDAO().getAll()).isEmpty();
  }

  @Test
  public void testUpdate_DoesNotDeletePullRequestsIfRepositoryUrlIsNotChanged() {
    // given a root org with github as a provider
    createRootOrgWithGitHubProvider();
    // And a source control and a pull request
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL);
    tempEntity.newSourceControlPullRequest(VALID_URL, 1, "testCommitHash", "testBranch", new Date(), new Date(),
        new Date());

    // Then update should not delete pull requests
    sourceControl.setToken(sourceControl.getToken() + "Updated");
    sourceControlDAO.update(sourceControl);
    assertThat(new SourceControlPullRequestDAO().getAll()).hasSize(1);
  }
}
