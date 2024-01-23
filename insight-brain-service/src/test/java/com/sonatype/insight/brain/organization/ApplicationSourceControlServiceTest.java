/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApplicationTotalRiskDTO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.PlexusCipher;

import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApplicationSourceControlServiceTest
    extends AbstractComponentTest
{
  private static final int LIMIT_LARGER_THAN_RESULT_SIZE = 100;

  private static final String ROOT_TOKEN = "root-token";

  private static final String ENC = "CMMDwoV";

  private static final String REPO_URL = "https://example.com/organization/project";

  @Inject
  private ApplicationSourceControlService applicationSourceControlService;

  @Inject
  private PlexusCipher plexusCipher;

  private Organization org;

  @Before
  public void before() throws Exception {
    org = tempEntity.newOrganization();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt(ROOT_TOKEN, ENC),
        SourceControlProvider.GITHUB);
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_ScmEnabled_h2() {
    final Application app1 = tempEntity.newApplication(org.getId());
    final Application app2 = tempEntity.newApplication(org.getId());

    // Add a source control record for app1 with ASCF enabled, so it shouldn't be in the result list
    // app2 has no source control record, so it should be in the result list
    tempEntity.newSourceControl(app1.getId(), REPO_URL, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, true);

    final List<?> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled).hasSize(1);
    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .extracting("applicationPublicId", "applicationName", "totalRisk")
        .containsExactly(new Tuple(app2.getPublicId(), app2.getName(), 0));
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_ScmEnabled_postgres() {
    final Application app1 = tempEntity.newApplication(org.getId());
    final Application app2 = tempEntity.newApplication(org.getId());

    // Add a source control record for app1 with ASCF enabled, so it shouldn't be in the result list
    // app2 has no source control record, so it should be in the result list
    tempEntity.newSourceControl(app1.getId(), REPO_URL, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, true);

    final List<?> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled).hasSize(1);
    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .extracting("applicationPublicId", "applicationName", "totalRisk")
        .containsExactly(new Tuple(app2.getPublicId(), app2.getName(), 0));
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_ScmDisabled_h2() {
    final Application app1 = tempEntity.newApplication(org.getId());
    final Application app2 = tempEntity.newApplication(org.getId());

    // Add a source control record for app1 with ASCF disabled, so it should be in the result list
    // app2 has no source control record, so it should be in the result list
    tempEntity.newSourceControl(app1.getId(), REPO_URL, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, false);

    final List<?> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled).hasSize(2);
    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .extracting("applicationPublicId", "applicationName")
        .contains(
            new Tuple(app1.getPublicId(), app1.getName())
        )
        .contains(new Tuple(app2.getPublicId(), app2.getName()));
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_ScmDisabled_postgres() {
    final Application app1 = tempEntity.newApplication(org.getId());
    final Application app2 = tempEntity.newApplication(org.getId());

    // Add a source control record for app1 with ASCF disabled, so it should be in the result list
    // app2 has no source control record, so it should be in the result list
    tempEntity.newSourceControl(app1.getId(), REPO_URL, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, false);

    final List<?> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled).hasSize(2);
    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .extracting("applicationPublicId", "applicationName")
        .contains(
            new Tuple(app1.getPublicId(), app1.getName())
        )
        .contains(new Tuple(app2.getPublicId(), app2.getName()));
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_AllAppsScmDisabled_h2() {
    // All apps missing source control records = SCM disabled
    final int numTotalApps = 8;
    final List<Application> applications = givenApplicationsWithAscendingRisk(numTotalApps);
    Collections.reverse(applications);

    final List<ApplicationTotalRiskDTO> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertResultsLimitedAfterSortingByRisk(appsWithAutomatedSourceControlFeedbackDisabled, applications, numTotalApps,
        7);
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_AllAppsScmDisabled_postgres() {
    // All apps missing source control records = SCM disabled
    final int numTotalApps = 8;
    final List<Application> applications = givenApplicationsWithAscendingRisk(numTotalApps);
    Collections.reverse(applications);

    final List<ApplicationTotalRiskDTO> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertResultsLimitedAfterSortingByRisk(appsWithAutomatedSourceControlFeedbackDisabled, applications, numTotalApps,
        7);
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_NoApps_h2() {
    final List<?> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .isEmpty();
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_NoApps_postgres() {
    final List<?> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .isEmpty();
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_ShouldIncludeApplicationTotalRisk() {
    final Application appWithOneViolation = tempEntity.newApplication(org.getId());
    givenAppHasPolicyViolation(appWithOneViolation, "app2 - one policy violation", 10);

    final Application appWithTwoViolations = tempEntity.newApplication(org.getId());
    givenAppHasPolicyViolation(appWithTwoViolations, "app1 - first policy violation", 5);
    givenAppHasPolicyViolation(appWithTwoViolations, "app1 - second policy violation", 2);

    final Application appWithOrgLevelPolicyViolations = tempEntity.newApplication(org.getId());
    givenAppOrgHasPolicyViolation(appWithOrgLevelPolicyViolations, org, "app3 -- org level violations", 8);

    final Application appWithNoViolations = tempEntity.newApplication(org.getId());

    final List<?> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .extracting("applicationPublicId", "applicationName", "totalRisk")
        .containsExactly(
            new Tuple(appWithOneViolation.getPublicId(), appWithOneViolation.getName(), 10),
            new Tuple(appWithOrgLevelPolicyViolations.getPublicId(), appWithOrgLevelPolicyViolations.getName(), 8),
            new Tuple(appWithTwoViolations.getPublicId(), appWithTwoViolations.getName(), 7),
            new Tuple(appWithNoViolations.getPublicId(), appWithNoViolations.getName(), 0));
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_ShouldSortDescendingAndApplyLimit() {
    final int numTotalAppsWithoutSourceControl = 11;
    final List<Application> allApplications = givenApplicationsWithAscendingRisk(numTotalAppsWithoutSourceControl);

    int givenLimit = numTotalAppsWithoutSourceControl;
    List<ApplicationTotalRiskDTO> results =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(givenLimit);

    final List<Application> appsInExpectedSortOrder = reverseList(allApplications);

    assertResultsLimitedAfterSortingByRisk(results, appsInExpectedSortOrder, givenLimit, 10);

    givenLimit = 7;
    results =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(givenLimit);

    assertResultsLimitedAfterSortingByRisk(results, appsInExpectedSortOrder, givenLimit, 10);

    givenLimit = 1;
    results =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(givenLimit);

    assertResultsLimitedAfterSortingByRisk(results, appsInExpectedSortOrder, givenLimit, 10);
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_InvalidLimitSize() {
    assertThatThrownBy(
        () -> applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(-1))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Limit size must be greater than 0");
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_IncludeZeroRiskApps() {
    // Add three apps with no associated risk
    givenApplicationsWithNoRisk(3);

    final List<ApplicationTotalRiskDTO> results =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);

    assertThat(results).hasSize(3);
    results.forEach(app -> assertThat(app.totalRisk).isZero());
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_NullPRCommentingAndNullCommitEnabled() {
    createAppWithAutomatedSourceControlFeedbackValues(null, null);
    final List<ApplicationTotalRiskDTO> resultsWhenBothNull =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);
    assertThat(resultsWhenBothNull).hasSize(1);
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_NullCommitStatusEnabled() {
    createAppWithAutomatedSourceControlFeedbackValues(null, true);
    final List<ApplicationTotalRiskDTO> resultsWhenOnlyPullRequestNull =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);
    assertThat(resultsWhenOnlyPullRequestNull).hasSize(1);
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_NullPRCommentingEnabled() {
    createAppWithAutomatedSourceControlFeedbackValues(true, null);
    final List<ApplicationTotalRiskDTO> resultsWhenOnlyCommitStatusNull =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            LIMIT_LARGER_THAN_RESULT_SIZE);
    assertThat(resultsWhenOnlyCommitStatusNull).hasSize(0);
  }

  private List<Application> givenApplicationsWithNoRisk(final int numApplications) {
    return IntStream.range(0, numApplications)
        .mapToObj(i -> tempEntity.newApplication(org.getId()))
        .collect(Collectors.toList());
  }

  private List<Application> givenApplicationsWithAscendingRisk(final int numApplications) {
    return IntStream.range(0, numApplications)
        .mapToObj(i -> {
          final Application givenApp = tempEntity.newApplication(org.getId());

          givenAppHasPolicyViolation(givenApp, "app owned policy" + i, i);

          return givenApp;
        })
        .collect(Collectors.toList());
  }

  private void givenAppHasPolicyViolation(final Application app, final String appPolicyName, final int threatLevel) {
    final Policy appPolicy = tempEntity.newPolicy(app.getId(), appPolicyName, threatLevel);

    long time = System.currentTimeMillis() - 1000;
    final PolicyEvaluation appPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "app eval",
            new Date(time));

    tempEntity.newPolicyViolation(appPolicyEvaluation, appPolicy);

    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash-" + generateRandomHashCode(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));

    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, "hash-" + generateRandomHashCode(),
        MatchState.SIMILAR, false);
  }

  private void givenAppOrgHasPolicyViolation(
      final Application app,
      final Organization org,
      final String appPolicyName,
      final int threatLevel)
  {
    final Policy orgPolicy = tempEntity.newPolicy(org.getId(), appPolicyName, threatLevel);

    long time = System.currentTimeMillis() - 1000;
    final PolicyEvaluation appPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "app eval",
            new Date(time));

    tempEntity.newPolicyViolation(appPolicyEvaluation, orgPolicy);
  }

  private void assertResultsLimitedAfterSortingByRisk(
      final List<ApplicationTotalRiskDTO> results,
      final List<Application> givenTotalApplicationSetSortedByRiskDescending,
      final int expectedResultLimit,
      final int expectedHighestRisk
  )
  {
    final List<Tuple> expectedResults = IntStream.range(0, givenTotalApplicationSetSortedByRiskDescending.size())
        .mapToObj(i -> {
          final Application app = givenTotalApplicationSetSortedByRiskDescending.get(i);
          return new Tuple(app.getPublicId(), app.getName(), expectedHighestRisk - i);
        })
        .limit(expectedResultLimit)
        .collect(Collectors.toList());

    assertThat(results).hasSize(expectedResultLimit);

    assertThat(results.get(0).totalRisk).isEqualTo(expectedHighestRisk);

    expectedResults.forEach(expectedApp -> {
      assertThat(results)
          .extracting("applicationPublicId", "applicationName", "totalRisk")
          .contains(expectedApp);
    });
  }

  private String generateRandomHashCode() {
    return String.valueOf(randomUUID().hashCode());
  }

  private <T> List<T> reverseList(List<T> list) {
    final List<T> listCopy = new ArrayList<>(list);

    Collections.reverse(listCopy);

    return listCopy;
  }

  private void createAppWithAutomatedSourceControlFeedbackValues(
      final Boolean pullRequestCommentingEnabled,
      final Boolean commitStatusEnabled
  )
  {
    final Application appWithMissingScmEntries = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(
        appWithMissingScmEntries.getId(),
        REPO_URL,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        pullRequestCommentingEnabled,
        null,
        "/target/*",
        true,
        commitStatusEnabled);
  }
}
