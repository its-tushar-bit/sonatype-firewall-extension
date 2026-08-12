/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO.PULL_REQUEST_POLLING_INITIAL_OFFSET_MS;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SourceControlDAOTest
    extends AbstractDbDAOTest
{
  private static final String NULL_REPO_URL = null;

  private static final String VALID_URL = "https://example.com/organization/Project.git";

  private static final String VALID_NORMALIZED_URL = "https://example.com/organization/Project";

  private static final String VALID_SSH_URL = "git@example.com:organization/Project.git";

  private static final int INTERVAL_IN_HOURS = 24;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private SourceControlDAO sourceControlDAO;

  private Application app;

  private Organization org;

  private DAOSecretRotator daoSecretRotator;

  @Mock
  private SourceControlSshValidator sourceControlSshValidator;

  @Override
  @Before
  public void setup() {
    policyEvaluationDAO = daoFactory.createPolicyEvaluationDAO();

    sourceControlDAO = daoFactory.createSourceControlDAO();
    sourceControlPullRequestDAO = daoFactory.createSourceControlPullRequestDAO();

    daoSecretRotator = new DAOSecretRotator();

    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
  }

  @After
  public void cleanup() {
    if (sourceControlDAO != null) {
      sourceControlDAO.getAll().forEach(sourceControlDAO::delete);
    }
  }

  @Test
  public void testInitializePullRequestPollTimes_appWithOlderPolicyEvaluationAndNeedingPollTime() {
    // given: several policy evaluations at different times and a related source control without a poll time;
    // the oldest policy eval is older than the polling offset
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
    // the oldest policy eval is newer than the polling offset
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
  public void testGetNextRepositoriesToPoll() {
    // given: several source control entries with different pull request poll times
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    LocalDateTime dateTime = LocalDateTime.now();
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo1", toDate(dateTime));

    Application app2 = tempEntity.newApplication("app2", org.getId());
    SourceControl sourceControl2 = tempEntity.newSourceControl(app2.getId(), "http://a.com/org/repo2",
        toDate(dateTime.minusDays(1)));

    Application app3 = tempEntity.newApplication("app3", org.getId());
    tempEntity.newSourceControl(app3.getId(), "http://a.com/org/repo3",
        toDate(dateTime.minusDays(2)));

    // when: get repos to poll
    List<SourceControl> results = sourceControlDAO.getNextRepositoriesToPoll(10);

    // then: returns all 3 ordered by oldest poll time first
    assertThat(results).hasSize(3);
    assertThat(results.get(0).getOwnerId()).isEqualTo(app3.getId());
    assertThat(results.get(1).getOwnerId()).isEqualTo(app2.getId());
    assertThat(results.get(2).getOwnerId()).isEqualTo(app.getId());

    // when: clear app2 poll time
    sourceControl2.setPullRequestPollTime(null);
    sourceControlDAO.update(sourceControl2);
    results = sourceControlDAO.getNextRepositoriesToPoll(10);

    // then: app2 excluded, only 2 returned
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getOwnerId()).isEqualTo(app3.getId());
    assertThat(results.get(1).getOwnerId()).isEqualTo(app.getId());

    // when: limit is respected
    results = sourceControlDAO.getNextRepositoriesToPoll(1);

    // then: only the oldest is returned
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getOwnerId()).isEqualTo(app3.getId());
  }

  @Test
  public void testGetNextRepositoriesToPoll_future() {
    // given: source control entries with pull request poll time in future
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    LocalDateTime dateTime = LocalDateTime.now();
    SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo1", toDate(dateTime.plusDays(1)));

    // when: get repos to poll
    List<SourceControl> results = sourceControlDAO.getNextRepositoriesToPoll(10);

    // then: nothing should be returned
    assertThat(results).isEmpty();

    // when: update app source control poll time to have past time
    appSourceControl.setPullRequestPollTime(toDate(dateTime.minusDays(1)));
    sourceControlDAO.update(appSourceControl);
    results = sourceControlDAO.getNextRepositoriesToPoll(10);

    // then: app source control is returned
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getOwnerId()).isEqualTo(app.getId());
  }

  @Test
  public void testInsert_MissingOwnerId() {
    assertThatThrownBy(() -> sourceControlDAO.insert(new SourceControl.Builder().build()))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("SourceControl owner id is required");
  }

  @Test
  public void testInsert_RepositoryUrlForOrganization() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(org.getId()).setRepositoryUrl(VALID_URL).setToken("token").build();
    assertThatThrownBy(() -> sourceControlDAO.insert(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessage(
            "SourceControl repositoryUrl is not allowed for organization");
  }

  @Test
  public void testInsert_RootOrgDefaults() {
    String baseBranch = "development";

    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(ROOT_ORGANIZATION_ID)
        .setProvider(GITLAB)
        .setBaseBranch(baseBranch)
        .setRemediationPullRequestsEnabled(null)
        .setStatusChecksEnabled(null)
        .build();

    tempEntity.newSourceControl(sourceControl);

    SourceControl persistedSourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(persistedSourceControl.getRemediationPullRequestsEnabled()).isTrue();
    assertThat(persistedSourceControl.getStatusChecksEnabled()).isTrue();
  }

  @Test
  public void testInsert_RootOrgMissingDefaultBranch() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(ROOT_ORGANIZATION_ID)
        .setProvider(GITLAB)
        .build();

    assertThatThrownBy(() -> sourceControlDAO.insert(sourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("SourceControl baseBranch is required for the root organization");
  }

  @Test
  public void testInsert_MissingRepositoryUrlForApplication() {
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setToken("token").build();
    assertThatThrownBy(() -> sourceControlDAO.insert(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessage(
            "SourceControl repositoryUrl is required for application");
  }

  @Test
  public void testInsert_InvalidUrl() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl("https://not valid")
            .setToken("token")
            .build();
    assertThatThrownBy(() -> sourceControlDAO.insert(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("repositoryUrl is invalid");
  }

  @Test
  public void testInsert_CannotValidateUrl() {
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId())
            .setProvider(SourceControlProvider.GITHUB)
            .setRepositoryUrl("https://not valid")
            .build();
    assertThatThrownBy(() -> sourceControlDAO.insert(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl repositoryUrl is invalid");
  }

  @Test
  public void testInsert_AppPublicIdDoesNotExist() {
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId("baz")
            .setRepositoryUrl(VALID_URL)
            .setToken("bar")
            .build();
    assertThatThrownBy(() -> sourceControlDAO.insert(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl ownerId 'baz' cannot be found");
  }

  @Test
  public void testInsert_InvalidBaseBranchName() {
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId("baz")
        .setRepositoryUrl(VALID_URL)
        .setToken("bar")
        .setBaseBranch("/testBaseBranch")
        .build();
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testInsert_NullBaseBranchName() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken("bar")
        .setBaseBranch(null)
        .build();
    tempEntity.newSourceControl(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getBaseBranch()).isNull();
  }

  @Test
  public void testInsert_EmptyBaseBranchName() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken("bar")
        .setBaseBranch("")
        .build();
    tempEntity.newSourceControl(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getBaseBranch()).isEmpty();
  }

  @Test
  public void testInsert_DuplicateRepositoryUrlAllowed() {
    createRootOrgWithGitHubProvider();
    Application baz = tempEntity.newApplicationWithParent("baz");
    tempEntity.newSourceControl(baz.getId(), VALID_URL, "bar", null);
    sourceControlDAO
        .insert(new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken("bar")
            .build());
  }

  @Test
  public void testUpdate_MissingOwnerId() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", null);
    sourceControl.setOwnerId(null);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessage("SourceControl owner id is required");
  }

  @Test
  public void testUpdate_InvalidBaseBranchName() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", null);
    sourceControl.setBaseBranch("/testBaseBranch");
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testUpdate_NullBaseBranchName() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken("bar")
        .setBaseBranch("testBranchName")
        .build();
    tempEntity.newSourceControl(sourceControl);
    sourceControl.setBaseBranch(null);
    sourceControlDAO.update(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getBaseBranch()).isNull();
  }

  @Test
  public void testUpdate_EmptyBaseBranchName() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken("bar")
        .setBaseBranch("testBranchName")
        .build();
    tempEntity.newSourceControl(sourceControl);
    sourceControl.setBaseBranch("");
    sourceControlDAO.update(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getBaseBranch()).isEmpty();
  }

  @Test
  public void testUpdate_MissingRepositoryUrlForApplication() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "bar", null);
    sourceControl.setRepositoryUrl(null);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessage("SourceControl repositoryUrl is required for application");
  }

  @Test
  public void testUpdate_RepositoryUrlForOrganization() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(
        org.getId(), null, "bar", null);
    sourceControl.setRepositoryUrl(VALID_URL);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessage("SourceControl repositoryUrl is not allowed for organization");
  }

  @Test
  public void testUpdate_InvalidUrl() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", null);
    sourceControl.setRepositoryUrl("https://not valid");
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("repositoryUrl is invalid");
  }

  @Test
  public void testInsert_sshValidatorCalled() {
    sourceControlDAO = daoFactory.createSourceControlDAO(sourceControlSshValidator);
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId())
            .setProvider(SourceControlProvider.GITHUB)
            .setRepositoryUrl(VALID_URL)
            .setSshEnabled(true)
            .build();

    sourceControlDAO.insert(sourceControl);

    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNotNull();
    verify(sourceControlSshValidator, times(1)).validate(any());
  }

  @Test
  public void testUpdate_sshValidatorCalled() {
    sourceControlDAO = daoFactory.createSourceControlDAO(sourceControlSshValidator);
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId())
            .setProvider(SourceControlProvider.GITHUB)
            .setRepositoryUrl(VALID_URL)
            .setSshEnabled(true)
            .build();

    sourceControlDAO.insert(sourceControl);
    sourceControl.setSshEnabled(false);
    sourceControlDAO.update(sourceControl);

    assertThat(sourceControlDAO.getById(sourceControl.getId()).getSshEnabled()).isFalse();
    verify(sourceControlSshValidator, times(2)).validate(any());
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
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken("bar")
            .setBaseBranch("base/branch")
            .setRemediationPullRequestsEnabled(true)
            .setStatusChecksEnabled(true)
            .build();

    assertThat(sourceControl.getId()).isNull();
    tempEntity.newSourceControl(sourceControl);
    assertThat(sourceControl.getId()).isNotNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getOwnerId()).isEqualTo(app.getId());
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(sourceControl.getNormalizedRepositoryUrl()).isEqualTo(VALID_NORMALIZED_URL);
    assertThat(sourceControl.getUsername()).isNull();
    assertThat(sourceControl.getToken()).isEqualTo("bar");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("base/branch");
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isTrue();
    assertThat(sourceControl.getStatusChecksEnabled()).isTrue();

    sourceControl.setToken("baz");
    sourceControl.setBaseBranch("another");
    sourceControl.setRemediationPullRequestsEnabled(false);
    sourceControl.setStatusChecksEnabled(false);
    sourceControlDAO.update(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getUsername()).isNull();
    assertThat(sourceControl.getToken()).isEqualTo("baz");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("another");
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isFalse();
    assertThat(sourceControl.getStatusChecksEnabled()).isFalse();

    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testCRUD_Organization() {
    createRootOrgWithGitHubProvider();

    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(org.getId())
            .setToken("bar")
            .setRemediationPullRequestsEnabled(true)
            .setStatusChecksEnabled(true)
            .setBaseBranch("base/branch")
            .build();

    assertThat(sourceControl.getId()).isNull();
    tempEntity.newSourceControl(sourceControl);
    assertThat(sourceControl.getId()).isNotNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getOwnerId()).isEqualTo(org.getId());
    assertThat(sourceControl.getUsername()).isNull();
    assertThat(sourceControl.getToken()).isEqualTo("bar");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("base/branch");
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isTrue();
    assertThat(sourceControl.getStatusChecksEnabled()).isTrue();

    sourceControl.setToken("baz");
    sourceControl.setBaseBranch("another");
    sourceControl.setRemediationPullRequestsEnabled(false);
    sourceControl.setStatusChecksEnabled(false);
    sourceControlDAO.update(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getUsername()).isNull();
    assertThat(sourceControl.getToken()).isEqualTo("baz");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("another");
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isFalse();
    assertThat(sourceControl.getStatusChecksEnabled()).isFalse();

    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testInsert_PullRequestConfigsCanBeNull() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken("bar")
            .build();

    assertThat(sourceControl.getId()).isNull();
    assertThat(sourceControl.getBaseBranch()).isNull();
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isNull();
    assertThat(sourceControl.getStatusChecksEnabled()).isNull();

    tempEntity.newSourceControl(sourceControl);

    assertThat(sourceControl.getId()).isNotNull();
    assertThat(sourceControl.getBaseBranch()).isNull();
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isNull();
    assertThat(sourceControl.getStatusChecksEnabled()).isNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getBaseBranch()).isNull();
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isNull();
    assertThat(sourceControl.getStatusChecksEnabled()).isNull();
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
    tempEntity.newSourceControl(
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).build());
  }

  @Test
  public void testInsert_ProviderFromRootOrganization() {
    tempEntity.newSourceControl(org.getParentOrganizationId(), null, "token", SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(
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
    tempEntity.newSourceControl(sourceControl);
    sourceControl.setProvider(null);
    sourceControl.setToken(null);
    sourceControlDAO.update(sourceControl);
  }

  @Test
  public void testUpdate_ProviderNotAvailable() {
    SourceControl root = createRootOrgWithGitHubProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(VALID_URL)
            .setToken("TOKEN")
            .build();
    tempEntity.newSourceControl(sourceControl);
    sourceControl.setProvider(null);
    sourceControl.setToken(null);
    sourceControlDAO.delete(root);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The root organization source control provider is not set");
  }

  @Test
  public void testUpdate_ApplicationWithProvider() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .build();
    tempEntity.newSourceControl(sourceControl);
    sourceControl.setProvider(GITLAB);
    sourceControlDAO.update(sourceControl);
    sourceControl.setToken("token");
    sourceControlDAO.update(sourceControl);

    // Bitbucket has a different URL structure, expect an error even though only provider is changed
    sourceControl.setProvider(BITBUCKET);
    assertThatThrownBy(
        () -> sourceControlDAO.update(sourceControl))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining(
                "Expecting a valid Bitbucket server clone url in the form https://<domain>/scm/<project>/<repo>");
  }

  @Test
  public void testGetByApplication() {
    createRootOrgWithGitHubProvider();
    // create a few sample entries
    SourceControl scApp1 = buildAppSourceControl(app.getId(), 1, null);
    SourceControl scApp2 = buildAppSourceControlAndApp(org, 2, null);
    SourceControl scApp3 = buildAppSourceControlAndApp(org, 3, null);

    // create source control entries for organizations which will not have repository URLs set
    buildOrgSourceControl(org.getId(), null);

    assertThat(sourceControlDAO.getByApplication())
        .hasSize(3)
        .extracting(SourceControl::getId)
        .containsExactlyInAnyOrder(scApp1.getId(), scApp2.getId(), scApp3.getId());
  }

  @Test
  public void testGetApplicationsWithPullReqsEnabled_enabledAtRoot() {
    // create SCM entry at root, with PR enabled flag set
    buildOrgSourceControl(Organization.ROOT_ORGANIZATION_ID, true);

    // create a NULL PR org under root org
    Organization orgNullPrs = tempEntity.newOrganization();
    buildOrgSourceControl(orgNullPrs.getId(), null);

    // create a NO(false) PR org under the NULL PR org
    Organization orgNoPrs = tempEntity.newOrganization(orgNullPrs);
    buildOrgSourceControl(orgNoPrs.getId(), false);

    // enabled at app level, disabled at org level => enabled
    SourceControl scEnabledAtAppDisabledAtOrg = buildAppSourceControlAndApp(orgNoPrs, 5, true);

    // default at app level, default org level, enabled root => enabled
    SourceControl scDefaultAppDefaultOrg = buildAppSourceControlAndApp(orgNullPrs, 6, null);

    // default at app level, disabled org level => not enabled
    buildAppSourceControlAndApp(orgNoPrs, 7, false);

    // create entries at the application level without the enable PR flag set. Use null & explicitly enable
    SourceControl scDefault = buildAppSourceControl(app.getId(), 1, null);
    SourceControl scExplicitlyEnabled = buildAppSourceControlAndApp(org, 2, true);

    // create application entries that are explicitly disabled
    buildAppSourceControlAndApp(org, 3, false);
    buildAppSourceControlAndApp(org, 4, false);

    Collection<SourceControl> enabledApplications =
        sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled();
    assertThat(enabledApplications).extracting(SourceControl::getId)
        .containsExactlyInAnyOrder(
            scExplicitlyEnabled.getId(), scDefault.getId(), scEnabledAtAppDisabledAtOrg.getId(),
            scDefaultAppDefaultOrg.getId());
  }

  @Test
  public void testGetApplicationsWithRemediationPullRequestsEnabled_enabledAtOrgAndApp() {
    createRootOrgWithGitHubProvider();

    // create a NULL PR org under root org
    Organization orgNullPrs = tempEntity.newOrganization();
    buildOrgSourceControl(orgNullPrs.getId(), null);

    // create a NO(false) PR org under the NULL PR org
    Organization orgNoPrs = tempEntity.newOrganization(orgNullPrs);
    buildOrgSourceControl(orgNoPrs.getId(), false);

    // create an org under root, with PR enabled flag set
    Organization orgEnabledPrs = tempEntity.newOrganization();
    buildOrgSourceControl(orgEnabledPrs.getId(), true);

    // null at app level, enabled at org level => enabled
    SourceControl scEnabledAtOrg = buildAppSourceControlAndApp(orgEnabledPrs, 1, null);

    // enabled at app level, disabled at org level => enabled
    SourceControl scEnabledAtAppDisabledAtOrg = buildAppSourceControlAndApp(orgNoPrs, 2, true);

    // default at app level, default org level => not enabled
    SourceControl scDefaultAppDefaultOrg = buildAppSourceControlAndApp(orgNullPrs, 3, null);

    // default at app level, disabled org level => not enabled
    buildAppSourceControlAndApp(orgNoPrs, 4, false);

    Collection<SourceControl> enabledApplications =
        sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled();
    assertThat(enabledApplications).extracting(SourceControl::getId)
        .hasSize(3)
        .containsExactlyInAnyOrder(scEnabledAtAppDisabledAtOrg.getId(), scEnabledAtOrg.getId(),
            scDefaultAppDefaultOrg.getId());
  }

  private SourceControl buildAppSourceControlAndApp(
      Organization organization,
      int appNumber,
      Boolean remediationPullRequestsEnabled)
  {
    return buildAppSourceControl(tempEntity.newApplication(organization.getId()).getId(), appNumber,
        remediationPullRequestsEnabled);
  }

  private SourceControl buildAppSourceControl(String ownerId, int appNumber, Boolean remediationPullRequestsEnabled) {
    return buildAppSourceControl(ownerId, appNumber, remediationPullRequestsEnabled, null);
  }

  private SourceControl buildAppSourceControl(
      String ownerId,
      int appNumber,
      Boolean remediationPullRequestsEnabled,
      String token)
  {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(ownerId)
        .setRepositoryUrl("http://localhost/owner/app" + appNumber)
        .setRemediationPullRequestsEnabled(remediationPullRequestsEnabled)
        .setToken(token)
        .build();
    return tempEntity.newSourceControl(sourceControl);
  }

  private SourceControl buildOrgSourceControl(String ownerId, Boolean remediationPullRequestsEnabled) {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(ownerId)
        .setToken("token")
        .setRemediationPullRequestsEnabled(remediationPullRequestsEnabled)
        .build();
    if (Organization.ROOT_ORGANIZATION_ID.equals(ownerId)) {
      sourceControl.setProvider(GITHUB);
      sourceControl.setBaseBranch("main");
    }
    return tempEntity.newSourceControl(sourceControl);
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
        .setRemediationPullRequestsEnabled(false)
        .setStatusChecksEnabled(false)
        .build();

    tempEntity.newSourceControl(sourceControl);

    SourceControl persistedSourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(persistedSourceControl.getRemediationPullRequestsEnabled()).isFalse();
    assertThat(persistedSourceControl.getStatusChecksEnabled()).isFalse();

    persistedSourceControl.setRemediationPullRequestsEnabled(null);
    persistedSourceControl.setStatusChecksEnabled(null);

    sourceControlDAO.update(persistedSourceControl);

    SourceControl updatedSourceControl = sourceControlDAO.getById(persistedSourceControl.getId());
    assertThat(updatedSourceControl.getRemediationPullRequestsEnabled()).isTrue();
    assertThat(updatedSourceControl.getStatusChecksEnabled()).isTrue();
  }

  @Test
  public void testUpdate_RootOrgMissingDefaultBranch() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(ROOT_ORGANIZATION_ID)
        .setProvider(GITLAB)
        .setBaseBranch("master")
        .build();

    tempEntity.newSourceControl(sourceControl);

    SourceControl persistedSourceControl = sourceControlDAO.getById(sourceControl.getId());
    persistedSourceControl.setBaseBranch(null);

    assertThatThrownBy(() -> sourceControlDAO.update(persistedSourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("SourceControl baseBranch is required for the root organization");
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
  public void testCreate_OrganizationWithProvider() {
    tempEntity.newSourceControl(
        new SourceControl.Builder().setOwnerId(org.getId()).setProvider(SourceControlProvider.GITHUB).build());
    assertThat(sourceControlDAO.getByOwnerId(org.getId()).getProvider()).isEqualTo(SourceControlProvider.GITHUB);
  }

  @Test
  public void testCreate_ApplicationWithProvider() {
    tempEntity.newSourceControl(
        new SourceControl.Builder()
            .setOwnerId(app.getId())
            .setProvider(SourceControlProvider.GITHUB)
            .setRepositoryUrl("http://localhost/org/app.git")
            .build());
    SourceControl persistedSourceControl = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(persistedSourceControl.getProvider()).isEqualTo(SourceControlProvider.GITHUB);
  }

  @Test
  public void testInsert_OrganizationWithProvider() {
    tempEntity.newSourceControl(
        new SourceControl.Builder().setOwnerId(org.getId()).setProvider(SourceControlProvider.GITHUB).build());
    assertThat(sourceControlDAO.getByOwnerId(org.getId()).getProvider()).isEqualTo(GITHUB);
  }

  @Test
  public void testUpdate_OrganizationWithProvider() {
    createRootOrgWithGitHubProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(org.getId(), null, "token", null);
    sourceControl.setProvider(SourceControlProvider.GITHUB);
    sourceControlDAO.update(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId()).getProvider()).isEqualTo(GITHUB);
  }

  @Test
  public void testInsert_ApplicationWithProvider() {
    tempEntity.newSourceControl(
        new SourceControl.Builder().setOwnerId(app.getId())
            .setProvider(SourceControlProvider.GITHUB)
            .setRepositoryUrl("http://localhost:1234/org/repo")
            .build());
    assertThat(sourceControlDAO.getByOwnerId(app.getId()).getProvider()).isEqualTo(GITHUB);
  }

  @Test
  public void testUpdatePollTimeAndErrorCounts() {
    createRootOrgWithGitHubProvider();
    SourceControl scApp1 = buildAppSourceControl(app.getId(), 1, true);

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

    Application app1b = tempEntity.newApplication("my-app-2", org.getId());
    SourceControl scApp1b = buildAppSourceControl(app1b.getId(), 2, null);

    // define a token, so should be excluded from search results
    Application app1c = tempEntity.newApplication("my-app-3", org.getId());
    buildAppSourceControl(app1c.getId(), 3, null, "sample-token");

    // and several SC entries for another org / sub-org hierarchy
    Organization org2 = tempEntity.newOrganization();
    Application app2a = tempEntity.newApplication("my-app-2-1", org2.getId());
    SourceControl scApp2a = buildAppSourceControl(app2a.getId(), 1, null);

    Organization org21 = tempEntity.newOrganization(org2);
    Application app2b = tempEntity.newApplication("my-app-2-2", org21.getId());
    SourceControl scApp2b = buildAppSourceControl(app2b.getId(), 2, null);

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
  public void testGetApplicationSourceControlsWithInheritedCredentials() {
    new TestableHierarchy()
        // app1 with a custom token in its hierarchy
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "org1", "org2", "app1")
        .withProvider(GITLAB, null, null, null)
        .withToken("rootToken", null, "org2.token", null)
        .withDefaultBranch("rootBranch", null, null, null)
        .withRepositoryUrl("https://localhost:123/repo/app1", "https://localhost:123/repo/app1")
        // app 2 with custom provider
        .branchFrom("org1", "org3", "app2")
        .withRepositoryUrl("https://localhost:123/repo/app2", "https://localhost:123/repo/app2")
        .withProvider(null, GITHUB)
        // app 3 with custom token
        .branchFrom("org3", "org4", "app3")
        .withRepositoryUrl("https://localhost:123/repo/app3", "https://localhost:123/repo/app3")
        .withToken(null, "app3.token")
        // app 4 with with no credential customizations in its hierarchy
        .branchFrom("org4", "app4")
        .withRepositoryUrl("https://localhost:123/repo/app4", "https://localhost:123/repo/app4")
        // app 5 with no custom providers or tokens in the hierarchy
        .branchFrom("org4", "app5")
        .withRepositoryUrl("https://localhost:123/repo/app5", "https://localhost:123/repo/app5")
        .build();

    // when we try to get applications with only the default token
    List<SourceControl> appsWithDefaultInheritedCredentials =
        sourceControlDAO.getApplicationSourceControlsWithInheritedCredentials();

    // then it doesn't contain the apps with custom tokens or with orgs that have custom tokens
    assertThat(appsWithDefaultInheritedCredentials)
        .extracting(SourceControl::getOwnerId)
        .containsOnly("app4", "app5");
  }

  @Test
  public void test_getApplicationSourceControlsWithRepositoriesAndDefaultCredentials_testProviders() {
    // given root org with github as a provider and default tokens
    createRootOrgWithGitHubProvider();

    // given org with default provider and default tokens
    Organization orgDefaults = tempEntity.newOrganization();

    // given apps with defaults and with custom provider but null tokens
    Application appDefaults = tempEntity.newApplication(orgDefaults.getId());
    SourceControl scDefaults =
        tempEntity.newSourceControl(appDefaults.getId(), "http://localhost:1234/org/app-defaults");

    Application appAllCustom = tempEntity.newApplication(orgDefaults.getId());
    tempEntity.newSourceControl(appAllCustom.getId(), "http://localhost:1234/org/app-all-custom", null, GITLAB);

    // given org with gitlab as a provider and a null token
    Organization orgGitlab = tempEntity.newOrganization();
    tempEntity.newSourceControl(orgGitlab.getId(), null, null, GITLAB);

    // given apps in the gitlab org with default tokens and custom tokens
    Application appGitlabDefaults = tempEntity.newApplication(orgGitlab.getId());
    tempEntity.newSourceControl(appGitlabDefaults.getId(), "http://localhost:2233/gl/app");

    Application appGitlabCustom = tempEntity.newApplication(orgGitlab.getId());
    tempEntity.newSourceControl(appGitlabCustom.getId(), "http://localhost:2233/gl/app-custom", "token", null);

    // given org with gitlab as a provider and no credentials UNDER orgGitlab
    Organization orgGitlabDefaults = tempEntity.newOrganization(orgGitlab);
    tempEntity.newSourceControl(orgGitlabDefaults.getId(), null, null, GITLAB);

    // given an app which provides credentials
    Application appGitlabCreds = tempEntity.newApplication(orgGitlabDefaults.getId());
    tempEntity.newSourceControl(appGitlabCreds.getId(), "http://localhost:2233/gl/app-defaults", "token", null);

    // when we query for apps with default creds
    List<SourceControl> appsWithDefaultCreds =
        sourceControlDAO.getApplicationSourceControlsWithInheritedCredentials();

    // then it contains only the app which has default creds
    assertThat(appsWithDefaultCreds)
        .extracting(SourceControl::getId)
        .containsOnly(scDefaults.getId());
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
    tempEntity.newSourceControlPullRequest(VALID_URL, 1, "testCommitHash", "testCommitHash",
        "testBranch", "baseBranch");

    // Then delete should not cascade to pull requests (because there are two source control records with the same
    // repository URL).
    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlPullRequestDAO.getAll()).hasSize(1);

    // Then delete should cascade to pull requests (because there is only one source control record left with that
    // repository URL).
    sourceControlDAO.delete(sourceControl1);
    assertThat(sourceControlPullRequestDAO.getAll()).hasSize(0);
  }

  @Test
  public void testGetByRepositoryUrl() {
    // given: a source control entry with a URL
    String repoOwnerAndName = "testOrg/repoName";
    String githubUrl = "https://localhost:1234/" + repoOwnerAndName;
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(app.getId(), githubUrl, "", null);

    // when: find the source control entry by owner and name string
    List<SourceControl> sourceControlList = sourceControlDAO.getByRepositoryUrl(githubUrl);

    // then: we found it
    assertThat(sourceControlList).isNotNull();
    assertThat(sourceControlList.size()).isEqualTo(1);
    assertThat(sourceControlList.get(0).getOwnerId()).isEqualTo(app.getId());

    // when: add a 2nd source control entry for same repo and search again
    Application app2 = tempEntity.newApplication(app.getOrganizationId());
    tempEntity.newSourceControl(app2.getId(), githubUrl, null);
    sourceControlList = sourceControlDAO.getByRepositoryUrl(githubUrl);

    // then: we have 2 now
    assertThat(sourceControlList.size()).isEqualTo(2);
    assertThat(sourceControlList).extracting(SourceControl::getOwnerId)
        .containsExactlyInAnyOrder(app.getId(), app2.getId());

    // when: add a source control entry with same owner, but different host and search again
    String gitlabUrl = "https://localhost:2233/" + repoOwnerAndName;
    Application app3 = tempEntity.newApplication(app.getOrganizationId());
    tempEntity.newSourceControl(app3.getId(), gitlabUrl, null, GITLAB);
    sourceControlList = sourceControlDAO.getByRepositoryUrl(githubUrl);

    // then we still have 2
    assertThat(sourceControlList.size()).isEqualTo(2);
    assertThat(sourceControlList).extracting(SourceControl::getOwnerId)
        .containsExactlyInAnyOrder(app.getId(), app2.getId());
  }

  @Test
  public void testUpdate_DeletesOrphanPullRequestsIfRepositoryUrlIsChanged() {
    // given a root org with github as a provider
    createRootOrgWithGitHubProvider();
    // And two source controls (for two apps) with the same repository URL
    Application app1 = tempEntity.newApplicationWithParent();
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL);
    SourceControl sourceControl1 = tempEntity.newSourceControl(app1.getId(), VALID_URL);
    tempEntity.newSourceControlPullRequest(VALID_URL, 1, "testCommitHash", "testCommitHash",
        "testBranch", "baseBranch");

    // Then update should not delete pull requests (because there were two source control records with the same
    // repository URL).
    sourceControl.setRepositoryUrl(VALID_URL + "Updated");
    sourceControlDAO.update(sourceControl);
    assertThat(sourceControlPullRequestDAO.getAll()).hasSize(1);

    // Then update should delete pull requests (because there is only one source control record left with that
    // repository URL).
    sourceControl1.setRepositoryUrl(VALID_URL + "Updated1");
    sourceControlDAO.update(sourceControl1);
    assertThat(sourceControlPullRequestDAO.getAll()).isEmpty();
  }

  @Test
  public void testUpdate_DoesNotDeletePullRequestsIfRepositoryUrlIsNotChanged() {
    // given a root org with github as a provider
    createRootOrgWithGitHubProvider();
    // And a source control and a pull request
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL);
    tempEntity.newSourceControlPullRequest(VALID_URL, 1, "testCommitHash", "testCommitHash",
        "testBranch", "baseBranch");

    // Then update should not delete pull requests
    sourceControl.setToken(sourceControl.getToken() + "Updated");
    sourceControlDAO.update(sourceControl);
    assertThat(sourceControlPullRequestDAO.getAll()).hasSize(1);
  }

  @Test
  public void testUpdate_ClearsSshUrlIfRepositoryUrlIsChanged() {
    // given a root org with github as a provider
    createRootOrgWithGitHubProvider();
    // and a SC for an app with a SSH URL
    SourceControl sourceControl = tempEntity.newSourceControl(new SourceControl.Builder()
        .setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .setRepositorySshUrl(VALID_SSH_URL)
        .setSshEnabled(true)
        .build());

    // when we update the repo URL
    sourceControl.setRepositoryUrl(VALID_URL + "Updated");
    sourceControlDAO.update(sourceControl);

    // then the ssh URL gets cleared
    SourceControl retrievedSourceControl = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(retrievedSourceControl.getRepositorySshUrl()).isNull();

    // and other attributes are unaffected
    assertThat(retrievedSourceControl.getSshEnabled()).isTrue();
  }

  @Test
  public void testUpdate_SshUrlUnchangedIfRepositoryUrlNotChanged() {
    // given a root org with github as a provider
    createRootOrgWithGitHubProvider();
    // and a SC for an app with a SSH URL
    SourceControl sourceControl = tempEntity.newSourceControl(new SourceControl.Builder()
        .setOwnerId(app.getId())
        .setRepositoryUrl(VALID_URL)
        .setRepositorySshUrl(VALID_SSH_URL)
        .setSshEnabled(true)
        .build());

    // when we update the source control, leaving repository URL unchanged
    sourceControl.setBaseBranch("new_base_branch");
    sourceControlDAO.update(sourceControl);

    // then the ssh URL is populated
    SourceControl retrievedSourceControl = sourceControlDAO.getByOwnerId(app.getId());
    assertThat(retrievedSourceControl.getRepositorySshUrl()).isEqualTo(VALID_SSH_URL);
    assertThat(retrievedSourceControl.getSshEnabled()).isTrue();
  }

  @Test
  public void testBuildCompositeSourceControlInApplication_noMatchingApp() {
    // given: a root organization source control
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, NULL_REPO_URL, "some token", GITLAB);

    // when: fetch the composite source control for an app that doesn't currently exist
    SourceControl fetchedSourceControl = sourceControlDAO.buildCompositeSourceControlInApplication("does not exist");

    // then: null result
    assertThat(fetchedSourceControl).isNull();
  }

  @Test
  public void testBuildCompositeSourceControlInApplication_inheritFromRoot() {
    // given: a hierarch with everything inheriting from root
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appComposite")
        .withProvider(GITLAB, null, null)
        .withToken("rootToken", null, null)
        .withDefaultBranch("trunk", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, null, null)
        .withRemediationPullRequests(true, null, null)
        .withSourceControlEvaluations(false, null, null)
        .withSsh(false, null, null)
        .withCommitStatusEnabled(false, null, null)
        .withManualPullRequestsEnabled(false, null, null)
        .withNonGoldenPullRequestsEnabled(true, null, null)
        .withStatusChecks(false, null, null)
        .build();

    SourceControl appSourceControl = sourceControlDAO.buildCompositeSourceControlInApplication(
        testableHierarchy.getApplication("appComposite").getId());

    assertSourceControl(
        appSourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(appSourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlInApplication_CommitStatusEnabled_AllNull() {
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appComposite")
        .withProvider(GITLAB, null, null)
        .withDefaultBranch("trunk", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withCommitStatusEnabled(null, null, null)
        .withManualPullRequestsEnabled(null, null, null)
        .build();

    SourceControl appSourceControl = sourceControlDAO.buildCompositeSourceControlInApplication(
        testableHierarchy.getApplication("appComposite").getId());

    assertThat(appSourceControl.getCommitStatusEnabled()).isNull();
  }

  @Test
  public void testBuildCompositeSourceControlInApplicationinheritFromIntermediaryOrg() {
    // given: a hierarch with everything inheriting from an intermediary org
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "org1", "org2", "org3", "appComposite")
        .withProvider(GITLAB, null, GITHUB, null, null)
        .withToken("rootToken", null, "org2.token", null, null)
        .withDefaultBranch("trunk", null, "main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, null, true, null, null)
        .withRemediationPullRequests(true, null, false, null, null)
        .withSourceControlEvaluations(false, null, true, null, null)
        .withSsh(false, null, true, null, null)
        .withCommitStatusEnabled(false, null, true, null, null)
        .withStatusChecks(false, null, true, null, null)
        .withManualPullRequestsEnabled(false, null, true, null, null)
        .branchFrom("org2", "org4", "app2")
        .withRepositoryUrl("https://test.sonatype.com/app/2", "ssh://test.sonatype.com/app/2.git")
        .withToken("org4.token", null)
        .build();

    SourceControl appSourceControl = sourceControlDAO.buildCompositeSourceControlInApplication(
        testableHierarchy.getApplication("appComposite").getId());

    assertSourceControl(
        appSourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(appSourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlInApplication_inheritFromIntermediaryOrg_startsFromOrg() {
    // given: a hierarch with everything inheriting from an intermediary org
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "org1", "org2", "org3", "appComposite")
        .withRepositoryUrl("https://test.sonatype.com/app/2", "ssh://test.sonatype.com/app/2.git")
        .withProvider(GITLAB, null, GITHUB, null, null)
        .withToken("rootToken", null, "org2.token", null, null)
        .withDefaultBranch("trunk", null, "main", null, null)
        .withPullRequestCommenting(false, null, true, null, null)
        .withRemediationPullRequests(true, null, false, null, null)
        .withSourceControlEvaluations(false, null, true, null, null)
        .withSsh(false, null, true, null, null)
        .withCommitStatusEnabled(false, null, true, null, null)
        .withStatusChecks(false, null, true, null, null)
        .withManualPullRequestsEnabled(false, null, true, null, null)
        .build();

    SourceControl org3SourceControl = sourceControlDAO.buildCompositeSourceControlInApplication("org3");

    assertSourceControl(
        org3SourceControl,
        testableHierarchy.getExpectedCompositeSourceControl("org3"));
  }

  @Test
  public void testBuildCompositeSourceControlInApplication_inheritFromOrg() {
    // given: a hierarch with everything inheriting from parent org
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appComposite")
        .withProvider(GITLAB, GITHUB, null)
        .withToken("rootToken", "gh-token", null)
        .withDefaultBranch("trunk", "main", null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, true, null)
        .withRemediationPullRequests(true, false, null)
        .withSourceControlEvaluations(false, true, null)
        .withSsh(false, true, null)
        .withCommitStatusEnabled(false, true, null)
        .withStatusChecks(false, true, null)
        .withManualPullRequestsEnabled(false, true, null)
        .withNonGoldenPullRequestsEnabled(null, true, null)
        .build();

    SourceControl appSourceControl = sourceControlDAO.buildCompositeSourceControlInApplication(
        testableHierarchy.getApplication("appComposite").getId());

    assertSourceControl(
        appSourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(appSourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlInApplication_overrideAll() {
    // given: a hierarch with everything overridden in the app source control
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appComposite")
        .withProvider(GITLAB, GITHUB, GITLAB)
        .withToken("rootToken", "gh-token", "gl-token")
        .withDefaultBranch("trunk", "main", "develop")
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, true, false)
        .withRemediationPullRequests(true, null, false)
        .withSourceControlEvaluations(false, null, true)
        .withSsh(false, true, false)
        .withCommitStatusEnabled(false, true, false)
        .withStatusChecks(false, null, true)
        .withManualPullRequestsEnabled(false, true, false)
        .withNonGoldenPullRequestsEnabled(true, null, false)
        .build();

    SourceControl appSourceControl = sourceControlDAO.buildCompositeSourceControlInApplication(
        testableHierarchy.getApplication("appComposite").getId());

    assertSourceControl(
        appSourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(appSourceControl.getOwnerId()));
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_getOutdatedApplicationWithOnboardingScan() {
    // given: application with outdated policy evaluation
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(INTERVAL_IN_HOURS + 1));
    SourceControl scRoot = tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    SourceControl expectedSourceControl = tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    // Adjust for values inherited from parents
    expectedSourceControl.setProvider(scRoot.getProvider());
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is retrieved
    assertThat(sourceControlList.size()).isEqualTo(1);
    assertSourceControl(sourceControlList.get(0), expectedSourceControl);
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_ScanTriggerType() {
    SourceControl rootOrgSourceControl =
        tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    SourceControl orgSourceControl = tempEntity.newSourceControl(app.getOrganizationId(), null, null, null,
        SourceControlProvider.GITLAB, false, false, "branchParentOrg", null, true, true, "/target/*");
    SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), "http://example.com/org/repo", null, null, null, null, null);
    // Adjust for values inherited from parents
    appSourceControl.setProvider(rootOrgSourceControl.getProvider());
    appSourceControl.setBaseBranch(orgSourceControl.getBaseBranch());
    appSourceControl.setRemediationPullRequestsEnabled(orgSourceControl.getRemediationPullRequestsEnabled());
    appSourceControl.setStatusChecksEnabled(orgSourceControl.getStatusChecksEnabled());
    appSourceControl.setPullRequestCommentingEnabled(true);
    appSourceControl.setSourceControlEvaluationsEnabled(true);
    appSourceControl.setSourceControlScanTarget("/target/*");
    for (ScanTriggerType scanTriggerType : ScanTriggerType.values()) {
      testGetCompositeSourceControlForOutdatedSourceScans(scanTriggerType, appSourceControl);
    }
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testBuildCompositeSourceControlForApplicationId_noMatchingApp_postgres() {
    // given: a root organization source control
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, NULL_REPO_URL, "fake token", GITHUB);

    // when: build the composite source control for an application that does not exist
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId("contrived app id");

    // then: null result
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_noMatchingApp_h2() {
    // given: a root organization source control
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, NULL_REPO_URL, "fake token", GITHUB);

    // when: build the composite source control for an application that does not exist
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId("contrived app id");

    // then: null result
    assertThat(sourceControl).isNull();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testBuildCompositeSourceControlForApplicationId_inheritFromRoot_postgres() {
    // given: a hierarchy with all attributes inheriting from the root org
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgID", "appOne")
        .withProvider(GITHUB, null, null)
        .withToken("fakeToken", null, null)
        .withDefaultBranch("main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, null, null)
        .withRemediationPullRequests(true, null, null)
        .withSourceControlEvaluations(false, null, null)
        .withSsh(false, null, null)
        .withCommitStatusEnabled(false, null, null)
        .withManualPullRequestsEnabled(false, null, null)
        .withStatusChecks(false, null, null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: composite source control is built successfully
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_inheritFromRoot_h2() {
    // given: a hierarchy with all attributes inheriting from the root org
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgID", "appOne")
        .withProvider(GITHUB, null, null)
        .withToken("fakeToken", null, null)
        .withDefaultBranch("main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, null, null)
        .withRemediationPullRequests(true, null, null)
        .withSourceControlEvaluations(false, null, null)
        .withSsh(false, null, null)
        .withCommitStatusEnabled(false, null, null)
        .withManualPullRequestsEnabled(false, null, null)
        .withStatusChecks(false, null, null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: composite source control is built successfully
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testBuildCompositeSourceControlForApplicationId_CommitStatusEnabled_AllNull_postgres() {
    // given: a hierarchy with commit status enabled set to null
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appOne")
        .withProvider(GITHUB, null, null)
        .withDefaultBranch("main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withCommitStatusEnabled(null, null, null)
        .withManualPullRequestsEnabled(null, null, null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: composite source control is built, with commitStatusEnabled having a null value
    assertThat(sourceControl.getCommitStatusEnabled()).isNull();
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_CommitStatusEnabled_AllNull_h2() {
    // given: a hierarchy with commit status enabled set to null
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appOne")
        .withProvider(GITHUB, null, null)
        .withDefaultBranch("main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withCommitStatusEnabled(null, null, null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: composite source control is built, with commitStatusEnabled having a null value
    assertThat(sourceControl.getCommitStatusEnabled()).isNull();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testBuildCompositeSourceControlForApplicationId_inheritFromIntermediateOrg_postgres() {
    // given: a hierarchy with multiple nested organizations
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "org1", "org2", "org3", "appOne")
        .withProvider(GITLAB, null, GITHUB, null, null)
        .withToken("rootToken", null, "org2.token", null, null)
        .withDefaultBranch("trunk", null, "main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, null, true, null, null)
        .withRemediationPullRequests(true, null, false, null, null)
        .withSourceControlEvaluations(false, null, true, null, null)
        .withSsh(false, null, true, null, null)
        .withCommitStatusEnabled(false, null, true, null, null)
        .withStatusChecks(false, null, true, null, null)
        .withManualPullRequestsEnabled(false, null, true, null, null)
        .branchFrom("org2", "org4", "app2")
        .withRepositoryUrl("https://test.sonatype.com/app/2", "ssh://test.sonatype.com/app/2.git")
        .withToken("org4.token", null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: source control is built correctly
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_inheritFromIntermediateOrg_h2() {
    // given: a hierarchy with multiple nested organizations
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "org1", "org2", "org3", "appOne")
        .withProvider(GITLAB, null, GITHUB, null, null)
        .withToken("rootToken", null, "org2.token", null, null)
        .withDefaultBranch("trunk", null, "main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, null, true, null, null)
        .withRemediationPullRequests(true, null, false, null, null)
        .withSourceControlEvaluations(false, null, true, null, null)
        .withSsh(false, null, true, null, null)
        .withCommitStatusEnabled(false, null, true, null, null)
        .withStatusChecks(false, null, true, null, null)
        .withManualPullRequestsEnabled(false, null, true, null, null)
        .branchFrom("org2", "org4", "app2")
        .withRepositoryUrl("https://test.sonatype.com/app/2", "ssh://test.sonatype.com/app/2.git")
        .withToken("org4.token", null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: source control is built correctly
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testBuildCompositeSourceControlForApplicationId_inheritFromOrg_postgres() {
    // given: a hierarchy with everything inheriting from parent org
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appOne")
        .withProvider(GITLAB, GITHUB, null)
        .withToken("rootToken", "gh-token", null)
        .withDefaultBranch("trunk", "main", null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, true, null)
        .withRemediationPullRequests(true, false, null)
        .withSourceControlEvaluations(false, true, null)
        .withSsh(false, true, null)
        .withCommitStatusEnabled(false, true, null)
        .withStatusChecks(false, true, null)
        .withManualPullRequestsEnabled(false, true, null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: source control is built correctly
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_inheritFromOrg_h2() {
    // given: a hierarchy with everything inheriting from parent org
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appOne")
        .withProvider(GITLAB, GITHUB, null)
        .withToken("rootToken", "gh-token", null)
        .withDefaultBranch("trunk", "main", null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, true, null)
        .withRemediationPullRequests(true, false, null)
        .withSourceControlEvaluations(false, true, null)
        .withSsh(false, true, null)
        .withCommitStatusEnabled(false, true, null)
        .withStatusChecks(false, true, null)
        .withManualPullRequestsEnabled(false, true, null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: source control is built correctly
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testBuildCompositeSourceControlForApplicationid_overrideAll_postgres() {
    // given: a hierarchy with everything overridden in the app source control
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appOne")
        .withProvider(GITLAB, GITHUB, GITLAB)
        .withToken("rootToken", "gh-token", "gl-token")
        .withDefaultBranch("trunk", "main", "develop")
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, true, false)
        .withRemediationPullRequests(true, null, false)
        .withSourceControlEvaluations(false, null, true)
        .withSsh(false, true, false)
        .withCommitStatusEnabled(false, true, false)
        .withStatusChecks(false, null, true)
        .withManualPullRequestsEnabled(false, true, false)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: source control is built correctly
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationid_overrideAll_h2() {
    // given: a hierarchy with everything overridden in the app source control
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appOne")
        .withProvider(GITLAB, GITHUB, GITLAB)
        .withToken("rootToken", "gh-token", "gl-token")
        .withDefaultBranch("trunk", "main", "develop")
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, true, false)
        .withRemediationPullRequests(true, null, false)
        .withSourceControlEvaluations(false, null, true)
        .withSsh(false, true, false)
        .withCommitStatusEnabled(false, true, false)
        .withStatusChecks(false, null, true)
        .withManualPullRequestsEnabled(false, true, false)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: source control is built correctly
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  private void testGetCompositeSourceControlForOutdatedSourceScans(
      ScanTriggerType scanTriggerType,
      SourceControl expectedSourceControl)
  {
    // given: application with outdated policy evaluation
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(INTERVAL_IN_HOURS + 1));
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId",
        false, false, false, scanTime, "commitHash123", scanTriggerType);

    try {
      // when: fetching source controls for applications with outdated source control policy evaluation
      List<SourceControl> sourceControlList =
          sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

      // then: source control is retrieved only for some trigger types
      if (scanTriggerType == ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING
          || scanTriggerType == ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING
          || scanTriggerType == ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST)
      {
        assertThat(sourceControlList).withFailMessage("Expected 1 result for scanTriggerType=%s", scanTriggerType)
            .hasSize(1);
        assertSourceControl(sourceControlList.get(0), expectedSourceControl);
      }
      else {
        assertThat(sourceControlList).withFailMessage("Expected no results for scanTriggerType=%s", scanTriggerType)
            .isEmpty();
      }
    }
    finally {
      policyEvaluationDAO.delete(policyEvaluation);
    }
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_getOutdatedApplicationWithInfoInRootOrg() {
    // given: application with outdated policy evaluation
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(INTERVAL_IN_HOURS + 1));
    SourceControl scRoot =
        tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, SourceControlProvider.GITLAB, true,
            true, "branchRootOrg", null, true, true, "/target/*");
    SourceControl expectedSourceControl = tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null,
        null, null, null, null);
    // Adjust values inherited from parents
    expectedSourceControl.setProvider(scRoot.getProvider());
    expectedSourceControl.setBaseBranch(scRoot.getBaseBranch());
    expectedSourceControl.setRemediationPullRequestsEnabled(scRoot.getRemediationPullRequestsEnabled());
    expectedSourceControl.setStatusChecksEnabled(scRoot.getStatusChecksEnabled());
    expectedSourceControl.setPullRequestCommentingEnabled(true);
    expectedSourceControl.setSourceControlEvaluationsEnabled(true);
    expectedSourceControl.setSourceControlScanTarget("/target/*");
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is retrieved
    assertThat(sourceControlList.size()).isEqualTo(1);
    assertSourceControl(sourceControlList.get(0), expectedSourceControl);
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_nonExistentSourcePolicyEvaluation() {
    // given: application without source policy evaluation
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(INTERVAL_IN_HOURS + 1));
    SourceControl scRoot = tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    SourceControl expectedSourceControl = tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    // Adjust values inherited from parents
    expectedSourceControl.setProvider(scRoot.getProvider());

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is retrieved
    assertThat(sourceControlList.size()).isEqualTo(1);
    assertSourceControl(sourceControlList.get(0), expectedSourceControl);

    // This checks that other stage policy evaluations different than source are ignored
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId2", false, false, false,
        scanTime, "commitHash1234");

    // when: fetching applications with outdated source control policy evaluation
    sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is retrieved
    assertThat(sourceControlList.size()).isEqualTo(1);
    assertSourceControl(sourceControlList.get(0), expectedSourceControl);
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_upToDateSourcePolicyEvaluations() {
    // given: application with up to date policy evaluation
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(INTERVAL_IN_HOURS - 1));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is ignored
    assertThat(sourceControlList).isNotNull();
    assertThat(sourceControlList.size()).isZero();
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_upToDateSourcePolicyEvaluations_nLevelOrgs() {
    // given: application with up to date policy evaluation
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(INTERVAL_IN_HOURS - 1));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    Application app1 = tempEntity.newApplication(org2.getId());

    tempEntity.newSourceControl(app1.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app1.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is ignored
    assertThat(sourceControlList).isNotNull();
    assertThat(sourceControlList.size()).isZero();
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_ignoreSourceControlScannedByCI() {
    // given: application with outdated policy evaluation
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(INTERVAL_IN_HOURS + 1));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.CONTINUOUS_INTEGRATION);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is ignored
    assertThat(sourceControlList.size()).isZero();
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_getOutdatedApplicationWithSourceAPIScan() {
    // given: application with outdated policy evaluation via Source API
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusDays(SourceControlDAO.EXTERNAL_EVALUATION_WINDOW_IN_DAYS + 1));
    SourceControl scRoot = tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    SourceControl expectedSourceControl = tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    // Adjust for values inherited from parents
    expectedSourceControl.setProvider(scRoot.getProvider());
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_API);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is retrieved
    assertThat(sourceControlList.size()).isEqualTo(1);
    assertSourceControl(sourceControlList.get(0), expectedSourceControl);
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_ignoreSourceControlScannedBySourceAPI() {
    // given: application with outdated policy evaluation
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusDays(SourceControlDAO.EXTERNAL_EVALUATION_WINDOW_IN_DAYS - 1));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "https://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_API);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is ignored
    assertThat(sourceControlList.size()).isZero();
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_includeSourceControlScannedByDBM() {
    // given: application with a policy evaluation within the DBM window, but executed by DBM
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(INTERVAL_IN_HOURS - 1));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    SourceControl expectedSourceControl = tempEntity.newSourceControl(app.getId(), "https://a.com/org/repo", null);
    // Adjust for values inherited from parents
    expectedSourceControl.setProvider(SourceControlProvider.GITLAB);

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is retrieved
    assertThat(sourceControlList.size()).isEqualTo(1);
    assertSourceControl(sourceControlList.get(0), expectedSourceControl);
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_ignoreSourceControlScannedByCI_nLevelOrgs() {
    // given: application with outdated policy evaluation
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(INTERVAL_IN_HOURS + 1));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    Application app1 = tempEntity.newApplication(org2.getId());

    tempEntity.newSourceControl(app1.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app1.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.CONTINUOUS_INTEGRATION);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is ignored
    assertThat(sourceControlList.size()).isZero();
  }

  @Test
  public void testGetCompositeSourceControlForOutdatedSourceScans_multipleOutdatedScans() {
    // given: multiple applications with and without outdated policy evaluation
    // application 1 with outdated scan
    LocalDateTime now = LocalDateTime.now();
    // This time will be recognized as outdated scan because is very close
    Date scanTime1 = toDate(now.minusHours(INTERVAL_IN_HOURS).minusNanos(1));
    SourceControl scRoot = tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    SourceControl expectedSourceControl1 = tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    // Adjust values inherited from parents
    expectedSourceControl1.setProvider(scRoot.getProvider());
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime1,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // application 2 with outdated scan - triggered by DBM
    Application app2 = tempEntity.newApplicationWithParent();
    Date scanTime2 = toDate(now.minusHours(INTERVAL_IN_HOURS + 1));
    SourceControl expectedSourceControl2 = tempEntity.newSourceControl(app2.getId(), "http://a.com/org/repo", null);
    // Adjust values inherited from parents
    expectedSourceControl2.setProvider(scRoot.getProvider());
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime2,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING);

    // application 3 without outdated scan
    Application app3 = tempEntity.newApplicationWithParent();
    Date scanTime3 = toDate(now.minusHours(INTERVAL_IN_HOURS - 1));
    tempEntity.newSourceControl(app3.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app3.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime3,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // when: fetching applications with outdated source control policy evaluation
    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(getScanLimitDate());

    // then: application is retrieved
    assertThat(sourceControlList.size()).isEqualTo(2);
    if (sourceControlList.get(0).getId().equals(expectedSourceControl2.getId())) {
      assertSourceControl(sourceControlList.get(0), expectedSourceControl2);
      assertSourceControl(sourceControlList.get(1), expectedSourceControl1);
    }
    else {
      assertSourceControl(sourceControlList.get(0), expectedSourceControl1);
      assertSourceControl(sourceControlList.get(1), expectedSourceControl2);
    }
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testRotateEncryptedSecrets() throws SQLException {
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);

    for (int i = 0; i < 4; i++) {
      Application app = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
      tempEntity.newSourceControl(app.getId(), "https://github.com/some/repo", "token_" + i + "_old",
          SourceControlProvider.GITHUB);
    }

    Function<String, String> secretRotator = secret -> secret.replace("old", "new");

    daoSecretRotator.rotateEncryptedSecrets(sourceControlDAO, secretRotator);

    List<SourceControl> results = sourceControlDAO.getAll();

    assertThat(results.stream().filter(sc -> sc.getToken() == null).count()).isEqualTo(1);
    assertThat(results.stream().filter(sc -> sc.getToken() != null).count()).isEqualTo(4);
    results.stream()
        .filter(sc -> sc.getToken() != null)
        .forEach(sc -> {
          assertThat(sc.getToken()).doesNotContain("old");
          assertThat(sc.getToken()).contains("new");
        });
  }

  @Test
  public void testGetCompositeSourceControlByOwnerId_AllNull() {
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appComposite")
        .withProvider(GITLAB, null, null)
        .withDefaultBranch("trunk", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withManualPullRequestsEnabled(null, null, null)
        .build();

    SourceControl appSourceControl = sourceControlDAO.buildCompositeSourceControlInApplication(
        testableHierarchy.getApplication("appComposite").getId());

    assertThat(appSourceControl.getManualPullRequestsEnabled()).isNull();
  }

  private void assertSourceControl(SourceControl actualSC, SourceControl expectedSC) {
    assertThat(actualSC.getId()).isEqualTo(expectedSC.getId());
    assertThat(actualSC.getOwnerId()).isEqualTo(expectedSC.getOwnerId());
    assertThat(actualSC.getRepositoryUrl()).isEqualTo(expectedSC.getRepositoryUrl());
    assertThat(actualSC.getUsername()).isEqualTo(expectedSC.getUsername());
    assertThat(actualSC.getToken()).isEqualTo(expectedSC.getToken());
    assertThat(actualSC.getProvider()).isEqualTo(expectedSC.getProvider());
    assertThat(actualSC.getBaseBranch()).isEqualTo(expectedSC.getBaseBranch());
    assertThat(actualSC.getRemediationPullRequestsEnabled()).isEqualTo(expectedSC.getRemediationPullRequestsEnabled());
    assertThat(actualSC.getStatusChecksEnabled()).isEqualTo(expectedSC.getStatusChecksEnabled());
    assertThat(actualSC.getPullRequestCommentingEnabled()).isEqualTo(expectedSC.getPullRequestCommentingEnabled());
    assertThat(actualSC.getPullRequestPollTime()).isEqualTo(expectedSC.getPullRequestPollTime());
    assertThat(actualSC.getPullRequestErrorCount()).isEqualTo(expectedSC.getPullRequestErrorCount());
    assertThat(actualSC.getSourceControlEvaluationsEnabled())
        .isEqualTo(expectedSC.getSourceControlEvaluationsEnabled());
    assertThat(actualSC.getSourceControlScanTarget()).isEqualTo(expectedSC.getSourceControlScanTarget());
    assertThat(actualSC.getManualPullRequestsEnabled()).isEqualTo(expectedSC.getManualPullRequestsEnabled());
    assertThat(actualSC.getNonGoldenPullRequestsEnabled()).isEqualTo(expectedSC.getNonGoldenPullRequestsEnabled());
  }

  private Date getScanLimitDate() {
    return toDate(LocalDateTime.now().minusHours(INTERVAL_IN_HOURS));
  }

  private class TestableHierarchy
  {
    private final List<SourceControl> sourceControlList = new ArrayList<>();

    private final Map<String, SourceControl> sourceControlMap = new HashMap<>();

    private final Map<String, String> childParentMap = new HashMap<>();

    private final Map<String, Application> applicationMap = new HashMap<>();

    private SourceControl currentAppSourceControl;

    private int hierarchyDepth;

    private int offset = 0;

    private Application getApplication(String applicationId) {
      return applicationMap.get(applicationId);
    }

    private SourceControl getSourceControl(int relativeIndex) {
      return sourceControlList.get(offset + relativeIndex);
    }

    private void setupHierarchy(String parent, String... ownerIds) {
      String currentParent = parent;
      for (String ownerId : ownerIds) {
        childParentMap.put(ownerId, currentParent);
        currentParent = ownerId;
      }
    }

    private TestableHierarchy branchFrom(String orgId, String... ownerIds) {
      setupHierarchy(orgId, ownerIds);
      offset = sourceControlList.size();
      hierarchyDepth = ownerIds.length;
      addOrgsAndApp(orgId, ownerIds);
      return this;
    }

    private TestableHierarchy with_N_OrgsAndAnApp(String... ownerIds) {
      assertThat(ROOT_ORGANIZATION_ID).isEqualTo(ownerIds[0]);
      hierarchyDepth = ownerIds.length;
      setupHierarchy(null, ownerIds);

      SourceControl sc = new SourceControl();
      sc.setOwnerId(ROOT_ORGANIZATION_ID);
      sourceControlList.add(sc);
      sourceControlMap.put(ROOT_ORGANIZATION_ID, sc);

      return addOrgsAndApp(ROOT_ORGANIZATION_ID, Arrays.copyOfRange(ownerIds, 1, ownerIds.length));
    }

    private TestableHierarchy addOrgsAndApp(String parentOwnerId, String... ownerIds) {
      for (int i = 0; i < ownerIds.length - 1; i++) {
        Organization org = tempEntity.newOrganizationWithSpecificIdAndParent(ownerIds[i], ownerIds[i], parentOwnerId);

        SourceControl sc = new SourceControl();
        sc.setOwnerId(org.getId());
        sourceControlList.add(sc);
        sourceControlMap.put(org.getId(), sc);

        parentOwnerId = ownerIds[i];
      }
      String applicationId = ownerIds[ownerIds.length - 1];
      Application app =
          tempEntity.newApplicationWithSpecificId(applicationId, applicationId, applicationId, parentOwnerId);
      applicationMap.put(applicationId, app);
      currentAppSourceControl = new SourceControl();
      currentAppSourceControl.setOwnerId(app.getId());
      sourceControlList.add(currentAppSourceControl);
      sourceControlMap.put(app.getId(), currentAppSourceControl);
      return this;
    }

    private TestableHierarchy withRepositoryUrl(String repositoryUrl, String sshUrl) {
      currentAppSourceControl.setRepositoryUrl(repositoryUrl);
      currentAppSourceControl.setRepositorySshUrl(sshUrl);
      return this;
    }

    private TestableHierarchy withProvider(SourceControlProvider... providers) {
      assertHierarchyDepth(providers.length);
      for (int i = 0; i < providers.length; i++) {
        getSourceControl(i).setProvider(providers[i]);
      }
      return this;
    }

    private TestableHierarchy withToken(String... tokens) {
      assertHierarchyDepth(tokens.length);
      for (int i = 0; i < tokens.length; i++) {
        getSourceControl(i).setToken(tokens[i]);
      }
      return this;
    }

    private TestableHierarchy withDefaultBranch(String... branches) {
      assertHierarchyDepth(branches.length);
      for (int i = 0; i < branches.length; i++) {
        getSourceControl(i).setBaseBranch(branches[i]);
      }
      return this;
    }

    private TestableHierarchy withRemediationPullRequests(Boolean... remediationFlags) {
      assertHierarchyDepth(remediationFlags.length);
      for (int i = 0; i < remediationFlags.length; i++) {
        getSourceControl(i).setRemediationPullRequestsEnabled(remediationFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withPullRequestCommenting(Boolean... commentingFlags) {
      assertHierarchyDepth(commentingFlags.length);
      for (int i = 0; i < commentingFlags.length; i++) {
        getSourceControl(i).setPullRequestCommentingEnabled(commentingFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withSourceControlEvaluations(Boolean... sourceControlEvaluationFlags) {
      assertHierarchyDepth(sourceControlEvaluationFlags.length);
      for (int i = 0; i < sourceControlEvaluationFlags.length; i++) {
        getSourceControl(i).setSourceControlEvaluationsEnabled(sourceControlEvaluationFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withSsh(Boolean... sshFlags) {
      assertHierarchyDepth(sshFlags.length);
      for (int i = 0; i < sshFlags.length; i++) {
        getSourceControl(i).setSshEnabled(sshFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withCommitStatusEnabled(Boolean... commitStatusEnabledFlags) {
      assertHierarchyDepth(commitStatusEnabledFlags.length);
      for (int i = 0; i < commitStatusEnabledFlags.length; i++) {
        getSourceControl(i).setCommitStatusEnabled(commitStatusEnabledFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withStatusChecks(Boolean... statusCheckFlags) {
      assertHierarchyDepth(statusCheckFlags.length);
      for (int i = 0; i < statusCheckFlags.length; i++) {
        getSourceControl(i).setStatusChecksEnabled(statusCheckFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withManualPullRequestsEnabled(Boolean... manualPullRequestsEnabledFlags) {
      assertHierarchyDepth(manualPullRequestsEnabledFlags.length);
      for (int i = 0; i < manualPullRequestsEnabledFlags.length; i++) {
        getSourceControl(i).setManualPullRequestsEnabled(manualPullRequestsEnabledFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withNonGoldenPullRequestsEnabled(Boolean... nonGoldenPullRequestsEnabledFlags) {
      assertHierarchyDepth(nonGoldenPullRequestsEnabledFlags.length);
      for (int i = 0; i < nonGoldenPullRequestsEnabledFlags.length; i++) {
        getSourceControl(i).setNonGoldenPullRequestsEnabled(nonGoldenPullRequestsEnabledFlags[i]);
      }
      return this;
    }

    private void assertHierarchyDepth(int depth) {
      assertThat(depth).isEqualTo(hierarchyDepth);
    }

    private SourceControl getExpectedCompositeSourceControl(String appId) {
      SourceControl composite = new SourceControl();
      SourceControl sc = sourceControlMap.get(appId);
      SourceControl.coalesce(composite, sc);
      while (null != (sc = sourceControlMap.get(childParentMap.get(sc.getOwnerId())))) {
        SourceControl.coalesce(composite, sc);
      }
      return composite;
    }

    TestableHierarchy build() {
      List<SourceControl> list = new ArrayList<>();
      for (SourceControl sourceControl : sourceControlList) {
        list.add(tempEntity.newSourceControl(sourceControl));
      }
      sourceControlList.clear();
      sourceControlList.addAll(list);

      return this;
    }
  }
}
