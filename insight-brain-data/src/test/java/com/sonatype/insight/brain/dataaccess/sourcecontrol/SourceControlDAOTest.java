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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SourceControlDAOTest
    extends AbstractDbDAOTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

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
  public void testUpdatePullRequestPollTimes_appWithPolicyEvaluationAndNeedingPollTime() {
    // given: several policy evaluations at different times and a related source control without a poll time
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusDays(3));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId2", false, false, false,
        toDate(now.minusDays(2)), "commitHash1234");
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId", false, false, false, scanTime,
        "commitHash123");
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId3", false, false, false,
        toDate(now.minusDays(1)), "commitHash1235");

    // when: fetch source control
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: poll time is not set
    assertThat(sourceControl.getPullRequestPollTime()).isNull();

    // when: update poll times and fetch app source control
    sourceControlDAO.updatePullRequestPollTimes();
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: source control poll time for app was updated to the earliest policy eval time
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(scanTime);
  }

  @Test
  public void testUpdatePullRequestPollTimes_appWithPolicyEvaluationAndAlreadyHasPollTime() {
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
    sourceControlDAO.updatePullRequestPollTimes();
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: source control poll time for app was not updated
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(startTime);
  }

  @Test
  public void testUpdatePullRequestPollTimes_appWithRepoUrlAndNeedsPollTime() {
    // given: app source control without poll time and no related policy evals
    Date startTime = new Date();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);

    // when: fetch source control
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: poll time missing
    assertThat(sourceControl.getPullRequestPollTime()).isNull();

    // when: update poll times and refetch
    sourceControlDAO.updatePullRequestPollTimes();
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: new poll time was assigned
    assertThat(sourceControl.getPullRequestPollTime()).isNotNull();
    assertThat(sourceControl.getPullRequestPollTime()).isAfterOrEqualTo(startTime);
  }

  @Test
  public void testUpdatePullRequestPollTimes_sourceControlWithoutRepoUrlAndWithPollTimeSet() {
    // given: source control with null repo url and with poll time set
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(org.getId(), null, new Date());

    // when: fetch source control for app
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(org.getId());

    // then: poll time not null
    assertThat(sourceControl.getPullRequestPollTime()).isNotNull();

    // when: update poll times and re-fetch
    sourceControlDAO.updatePullRequestPollTimes();
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
  public void testUpdatePullRequestPollTime() {
    // given: source control entry for application
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);

    // when: fetch source control
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: found and poll time is null
    assertThat(sourceControl).isNotNull();
    assertThat(sourceControl.getPullRequestPollTime()).isNull();

    // when: update poll time and refetch
    Date pollTime = new Date();
    sourceControlDAO.updatePullRequestPollTime(sourceControl.getId(), pollTime);
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: poll time matches
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(pollTime);
  }

  @Test
  public void testUpdatePullRequestPollTimeForApplication() {
    // given: source control entry for application
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);

    // when: fetch source control
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: found and poll time is null
    assertThat(sourceControl).isNotNull();
    assertThat(sourceControl.getPullRequestPollTime()).isNull();

    // when: update poll time and refetch
    Date pollTime = new Date();
    sourceControlDAO.updatePullRequestPollTimeForApplication(app.getId(), pollTime);
    sourceControl = sourceControlDAO.getByOwnerId(app.getId());

    // then: poll time matches
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(pollTime);
  }

  @Test
  public void testInsert_MissingOwnerId() {
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(new SourceControl.Builder().build());
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl owner id is required");
  }

  @Test
  public void testInsert_RepositoryUrlForOrganization() {
    createRootOrgWithProvider();
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
    createRootOrgWithProvider();
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
    createRootOrgWithProvider();
    Application baz = tempEntity.newApplicationWithParent("baz");
    tempEntity.newSourceControl(baz.getId(), VALID_URL, "bar", null);
    sourceControlDAO
        .insert(new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("bar")
            .build());
  }

  @Test
  public void testUpdate_MissingOwnerId() {
    createRootOrgWithProvider();
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", null);
    sourceControl.setOwnerId(null);
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl owner id is required");
  }

  @Test
  public void testUpdate_MissingRepositoryUrlForApplication() {
    createRootOrgWithProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "bar", null);
    sourceControl.setRepositoryUrl(null);
    assertThatThrownBy(() ->
        sourceControlDAO.update(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage("SourceControl repositoryUrl is required for application");
  }

  @Test
  public void testUpdate_RepositoryUrlForOrganization() {
    SourceControl sourceControl = tempEntity.newSourceControl(
        org.getId(), null, "bar", null);
    sourceControl.setRepositoryUrl(VALID_URL);
    assertThatThrownBy(() ->
        sourceControlDAO.update(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage("SourceControl repositoryUrl is not allowed for organization");
  }

  @Test
  public void testUpdate_InvalidUrl() {
    createRootOrgWithProvider();
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", null);
    sourceControl.setRepositoryUrl("https://not valid");
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("repositoryUrl is invalid");
  }

  @Test
  public void testUpdate_DuplicateRepositoryUrlAllowed() {
    createRootOrgWithProvider();
    Application baz = tempEntity.newApplicationWithParent();
    Application foo = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(baz.getId(), VALID_URL, "bar", null);
    SourceControl sourceControl =
        tempEntity.newSourceControl(foo.getId(), VALID_URL + ".1", "bar", null);
    sourceControl.setRepositoryUrl(VALID_URL);
    sourceControlDAO.update(sourceControl);
  }

  @Test
  public void testCRUD() {
    createRootOrgWithProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("bar")
            .setBaseBranch("base/branch").setEnablePullRequests(true)
            .setEnableStatusChecks(true).build();

    assertThat(sourceControl.getId()).isNull();
    sourceControlDAO.insert(sourceControl);
    assertThat(sourceControl.getId()).isNotNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getOwnerId()).isEqualTo(app.getId());
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(VALID_URL);
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
    assertThat(sourceControl.getToken()).isEqualTo("baz");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("another");
    assertThat(sourceControl.getEnablePullRequests()).isFalse();
    assertThat(sourceControl.getEnableStatusChecks()).isFalse();

    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testCRUD_Organization() {
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(org.getId()).setToken("bar")
            .setEnablePullRequests(true).setEnableStatusChecks(true)
            .setBaseBranch("base/branch").build();

    assertThat(sourceControl.getId()).isNull();
    sourceControlDAO.insert(sourceControl);
    assertThat(sourceControl.getId()).isNotNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getOwnerId()).isEqualTo(org.getId());
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
    assertThat(sourceControl.getToken()).isEqualTo("baz");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("another");
    assertThat(sourceControl.getEnablePullRequests()).isFalse();
    assertThat(sourceControl.getEnableStatusChecks()).isFalse();

    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testPullRequestConfigsCanBeNull() {
    createRootOrgWithProvider();
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
    createRootOrgWithProvider();
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
    createRootOrgWithProvider();
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
    createRootOrgWithProvider();
    SourceControl sourceControl =
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("TOKEN").build();
    sourceControlDAO.insert(sourceControl);
    sourceControl.setProvider(null);
    sourceControl.setToken(null);
    sourceControlDAO.update(sourceControl);
  }

  @Test
  public void testUpdate_ProviderNotAvailable() {
    SourceControl root = createRootOrgWithProvider();
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
  public void test_getAllForApplications() {
    createRootOrgWithProvider();
    // create a few sample entries
    SourceControl scApp1 = buildAppSourceControl(app.getId(), 1, null);
    sourceControlDAO.insert(scApp1);

    SourceControl scApp2 = buildAppSourceControlAndApp(org, 2, null);
    sourceControlDAO.insert(scApp2);

    SourceControl scApp3 = buildAppSourceControlAndApp(org, 3, null);
    sourceControlDAO.insert(scApp3);

    // create source control entries for organizations which will not have repository URLs set
    sourceControlDAO.insert(buildOrgSourceControl(org.getId(), null));

    Assertions.assertThat(sourceControlDAO.getByApplication())
        .hasSize(3).extracting(SourceControl::getId)
        .containsExactlyInAnyOrder(scApp1.getId(), scApp2.getId(), scApp3.getId());
  }

  @Test
  public void test_getCountOfApplicationsWithPREnabled_enabledAtRoot() {
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
    Assertions.assertThat(enabledApplications).extracting(SourceControl::getId).containsExactlyInAnyOrder(
        scExplicitlyEnabled.getId(), scDefault.getId(), scEnabledAtAppDisabledAtOrg.getId(),
        scDefaultAppDefaultOrg.getId());
  }

  @Test
  public void test_getCountOfApplicationsWithPREnabled_enabledAtOrgAndApp() {
    createRootOrgWithProvider();
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
    Assertions.assertThat(enabledApplications).extracting(SourceControl::getId)
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
  public void testCreate_RootOrgWithoutProvider() {
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
    SourceControl sourceControl = createRootOrgWithProvider();
    sourceControl.setProvider(null);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider is required for the root organization");
  }

  @Test
  public void testCreate_OrganizationWithProvider() {
    assertThatThrownBy(
        () -> sourceControlDAO.insert(
            new SourceControl.Builder().setOwnerId(org.getId()).setProvider(SourceControlProvider.GITHUB).build()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider can only be specified on the root organization");
  }

  @Test
  public void testUpdate_OrganizationWithProvider() {
    createRootOrgWithProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(org.getId(), null, "token", null);
    sourceControl.setProvider(SourceControlProvider.GITHUB);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider can only be specified on the root organization");
  }

  @Test
  public void testCreate_ApplicationWithProvider() {
    assertThatThrownBy(
        () -> sourceControlDAO.insert(
            new SourceControl.Builder().setOwnerId(app.getId()).setProvider(SourceControlProvider.GITHUB).build()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider can only be specified on the root organization");
  }

  @Test
  public void testUpdate_ApplicationWithProvider() {
    createRootOrgWithProvider();
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL, null, null);
    sourceControl.setProvider(SourceControlProvider.GITHUB);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl provider can only be specified on the root organization");
  }

  private SourceControl createRootOrgWithProvider() {
    return tempEntity.newSourceControl(org.getParentOrganizationId(), null, null, SourceControlProvider.GITHUB);
  }

  private Date toDate(LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }
}
