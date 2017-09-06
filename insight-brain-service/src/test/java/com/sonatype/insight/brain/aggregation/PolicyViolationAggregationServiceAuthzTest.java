/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aggregation;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.common.collect.Sets;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyViolationAggregationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  // one hour in milliseconds
  private static final int ONE_HOUR = 1000 * 60 * 60;

  @Inject
  private PolicyViolationAggregationService policyViolationAggregationService;

  private LocalDate today = new LocalDate();

  @Before
  public void before() {
    // Snap out of PoC mode.
    String appId = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID).getId();
    Date twoYearsAgo = new LocalDate().minusYears(2).toDate(); // outside the 12 month window for success metrics
    tempEntity.newPolicyEvaluation(appId, StageTypes.STAGE_RELEASE.getId(), "scanId", twoYearsAgo);
  }

  @Test
  public void testGetMttrs_ExplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    assertEmptyResults(policyViolationAggregationService.getMttrs(null, Collections.singleton(app.getId())));
  }

  @Test
  public void testGetMttrs_ExplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    login();
    assertEmptyResults(policyViolationAggregationService.getMttrs(null, Collections.singleton(app.getId())));
  }

  @Test
  public void testGetMttrs_ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app.getId(), today, ONE_HOUR);
    createPolicyViolation(app2.getId(), today, ONE_HOUR * 2);
    grantReadPermission(app.getId());

    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());
    assertMttrResults(policyViolationAggregationService.getMttrs(null, appIds), today);
  }

  @Test
  public void testGetMttrs_ExplicitOrganizationFilter_Unauthenticated() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    assertEmptyResults(policyViolationAggregationService.getMttrs(Collections.singleton(org.getId()), null));
  }

  @Test
  public void testGetMttrs_ExplicitOrganizationFilter_Unauthorized() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    login();
    assertEmptyResults(policyViolationAggregationService.getMttrs(Collections.singleton(org.getId()), null));
  }

  @Test
  public void testGetMttrs_ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app.getId(), today, ONE_HOUR);
    createPolicyViolation(app2.getId(), today, ONE_HOUR * 2);

    grantReadPermission(app.getId());
    assertMttrResults(policyViolationAggregationService.getMttrs(Collections.singleton(org.getId()), null), today);
  }

  @Test
  public void testGetMttrs_ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    createPolicyViolation(app.getId(), today, ONE_HOUR);
    createPolicyViolation(app2.getId(), today, ONE_HOUR * 2);

    grantReadPermission(org.getId());

    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());
    assertMttrResults(policyViolationAggregationService.getMttrs(orgIds, null), today);
  }

  @Test
  public void testGetMttrs_ImplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    assertEmptyResults(policyViolationAggregationService.getMttrs(null, null));
  }

  @Test
  public void testGetMttrs_ImplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    login();
    assertEmptyResults(policyViolationAggregationService.getMttrs(null, null));
  }

  @Test
  public void testGetMttrs_ImplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app.getId(), today, ONE_HOUR);
    createPolicyViolation(app2.getId(), today, ONE_HOUR * 2);
    grantReadPermission(app.getId());
    assertMttrResults(policyViolationAggregationService.getMttrs(null, null), today);
  }

  @Test
  public void testGetAverages_ExplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    assertEmptyResults(policyViolationAggregationService.getAverages(null, Collections.singleton(app.getId())));
  }

  @Test
  public void testGetAverages_ExplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    login();
    assertEmptyResults(policyViolationAggregationService.getAverages(null, Collections.singleton(app.getId())));
  }

  @Test
  public void testGetAverages_ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app.getId(), today);
    createPolicyViolation(app2.getId(), today);

    grantReadPermission(app.getId());

    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());
    assertAveragesResults(policyViolationAggregationService.getAverages(null, appIds), today);
  }

  @Test
  public void testGetAverages_ExplicitOrganizationFilter_Unauthenticated() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    assertEmptyResults(policyViolationAggregationService.getAverages(Collections.singleton(org.getId()), null));
  }

  @Test
  public void testGetAverages_ExplicitOrganizationFilter_Unauthorized() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    login();
    assertEmptyResults(policyViolationAggregationService.getAverages(Collections.singleton(org.getId()), null));
  }

  @Test
  public void testGetAverages_ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app.getId(), today);
    createPolicyViolation(app2.getId(), today);

    grantReadPermission(app.getId());
    assertAveragesResults(policyViolationAggregationService.getAverages(Collections.singleton(org.getId()), null),
        today);
  }

  @Test
  public void testGetAverages_ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    createPolicyViolation(app.getId(), today);
    createPolicyViolation(app2.getId(), today);

    grantReadPermission(org.getId());

    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());
    assertAveragesResults(policyViolationAggregationService.getAverages(orgIds, null), today);
  }

  @Test
  public void testGetAverages_ImplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    assertEmptyResults(policyViolationAggregationService.getAverages(null, null));
  }

  @Test
  public void testGetAverages_ImplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId(), today, ONE_HOUR);
    login();
    assertEmptyResults(policyViolationAggregationService.getAverages(null, null));
  }

  @Test
  public void testGetAverages_ImplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app.getId(), today, ONE_HOUR);
    createPolicyViolation(app2.getId(), today, ONE_HOUR * 2);
    grantReadPermission(app.getId());
    assertAveragesResults(policyViolationAggregationService.getAverages(null, null), today);
  }

  @Test
  public void testGetApplicationCounts_ExplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId(), today);
    assertEmptyResults(
        policyViolationAggregationService.getApplicationCounts(null, Collections.singleton(app.getId())));
  }

  @Test
  public void testGetApplicationCounts_ExplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId(), today);
    login();
    assertEmptyResults(
        policyViolationAggregationService.getApplicationCounts(null, Collections.singleton(app.getId())));
  }

  @Test
  public void testGetApplicationCounts_ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app.getId(), today);
    createPolicyViolation(app2.getId(), today);
    grantReadPermission(app.getId());

    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());
    assertApplicationCountsResult(policyViolationAggregationService.getApplicationCounts(null, appIds));
  }

  @Test
  public void testGetApplicationCounts_ExplicitOrganizationFilter_Unauthenticated() {
    createPolicyViolation(app.getId(), today);
    assertEmptyResults(
        policyViolationAggregationService.getApplicationCounts(Collections.singleton(org.getId()), null));
  }

  @Test
  public void testGetApplicationCounts_ExplicitOrganizationFilter_Unauthorized() {
    createPolicyViolation(app.getId(), today);
    login();
    assertEmptyResults(
        policyViolationAggregationService.getApplicationCounts(Collections.singleton(org.getId()), null));
  }

  @Test
  public void testGetApplicationCounts_ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app.getId(), today);
    createPolicyViolation(app2.getId(), today);

    grantReadPermission(app.getId());
    assertApplicationCountsResult(
        policyViolationAggregationService.getApplicationCounts(Collections.singleton(org.getId()), null));
  }

  @Test
  public void testGetApplicationCounts_ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    createPolicyViolation(app.getId(), today);
    createPolicyViolation(app2.getId(), today);

    grantReadPermission(org.getId());

    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());
    assertApplicationCountsResult(policyViolationAggregationService.getApplicationCounts(orgIds, null));
  }

  @Test
  public void testGetApplicationCounts_ImplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId(), today);
    assertEmptyResults(policyViolationAggregationService.getApplicationCounts(null, null));
  }

  @Test
  public void testGetApplicationCounts_ImplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId(), today);
    login();
    assertEmptyResults(policyViolationAggregationService.getApplicationCounts(null, null));
  }

  @Test
  public void testGetApplicationCounts_ImplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    createPolicyViolation(app.getId(), today);
    createPolicyViolation(app2.getId(), today);
    grantReadPermission(app.getId());
    assertApplicationCountsResult(policyViolationAggregationService.getApplicationCounts(null, null));
  }

  private void createPolicyViolation(String appId, LocalDate today) {
    createPolicyViolation(appId, today, ONE_HOUR);
  }

  private void createPolicyViolation(String appId, LocalDate today, long violationResolutionTimeMs) {
    Date eval1Date = today.withDayOfMonth(2).minusMonths(1).toDateTimeAtStartOfDay().toDate();
    Date eval2Date = new Date(eval1Date.getTime() + violationResolutionTimeMs);

    Policy policy = tempEntity.newPolicy(appId, "test policy name", 5);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "eval1", eval1Date);
    tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "eval2", eval2Date);

    // violation appears in eval1 but is resolved in eval2
    tempEntity.newPolicyViolation(eval1, policy);
  }

  private void assertMttrResults(List<MttrDTO> mttrDTOs, LocalDate today) {
    assertThat(mttrDTOs, hasSize(1));

    MttrDTO dto = mttrDTOs.get(0);
    assertThat(dto.timePeriodStart, is(today.withDayOfMonth(1).minusMonths(1).toDateTimeAtStartOfDay().toDate()));
    assertThat(dto.mttrInSeconds, is(ONE_HOUR / 1000));
    assertThat(dto.criticalMttrInSeconds, is(nullValue()));
  }

  private void assertAveragesResults(SuccessMetricsAveragesDTO dto, LocalDate today) {
    List<AverageDiscoveredPolicyViolationsDTO> averagesDTOs = dto.averageDiscoveredPolicyViolations;

    assertThat(averagesDTOs, hasSize(1));

    AverageDiscoveredPolicyViolationsDTO averageDiscoveredPolicyViolations = averagesDTOs.get(0);
    assertThat(averageDiscoveredPolicyViolations.timePeriodStart,
        is(today.withDayOfMonth(1).minusMonths(1).toDateTimeAtStartOfDay().toDate()));
    assertThat(averageDiscoveredPolicyViolations.license.averageDiscoveredSevere, is(1.0));
    assertThat(averageDiscoveredPolicyViolations.evaluationCount, is(2));
    assertThat(dto.activeApplicationCount, is(1));
  }

  private void assertApplicationCountsResult(ApplicationCountsDTO dto) {
    assertThat(dto.totalApplications, is(1));
    assertThat(dto.activeApplications, is(1));
    assertThat(dto.total.applicationsWithViolations, is(1));
    assertThat(dto.total.applicationsWithCriticalViolations, is(0));
    assertThat(dto.security.applicationsWithViolations, is(0));
    assertThat(dto.security.applicationsWithCriticalViolations, is(0));
    assertThat(dto.license.applicationsWithViolations, is(1));
    assertThat(dto.license.applicationsWithCriticalViolations, is(0));
    assertThat(dto.quality.applicationsWithViolations, is(0));
    assertThat(dto.quality.applicationsWithCriticalViolations, is(0));
    assertThat(dto.other.applicationsWithViolations, is(0));
    assertThat(dto.other.applicationsWithCriticalViolations, is(0));
  }

  private void assertEmptyResults(SuccessMetricsAveragesDTO dto) {
    assertThat(dto.activeApplicationCount, is(0));
    assertEmptyResults(dto.averageDiscoveredPolicyViolations);
  }

  private void assertEmptyResults(ApplicationCountsDTO dto) {
    assertThat(dto.totalApplications, is(0));
    assertThat(dto.activeApplications, is(0));
    assertThat(dto.total.applicationsWithViolations, is(0));
    assertThat(dto.total.applicationsWithCriticalViolations, is(0));
    assertThat(dto.security.applicationsWithViolations, is(0));
    assertThat(dto.security.applicationsWithCriticalViolations, is(0));
    assertThat(dto.license.applicationsWithViolations, is(0));
    assertThat(dto.license.applicationsWithCriticalViolations, is(0));
    assertThat(dto.quality.applicationsWithViolations, is(0));
    assertThat(dto.quality.applicationsWithCriticalViolations, is(0));
    assertThat(dto.other.applicationsWithViolations, is(0));
    assertThat(dto.other.applicationsWithCriticalViolations, is(0));
  }

  private void assertEmptyResults(List<?> dtos) {
    assertThat(dtos, is(empty()));
  }
}
