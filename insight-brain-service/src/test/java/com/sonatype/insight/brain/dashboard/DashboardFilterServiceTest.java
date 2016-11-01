/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

public class DashboardFilterServiceTest
    extends AbstractComponentTest
{
  @Inject
  private DashboardFilterService dashboardFilterService;

  private Organization org;
  private Application app1;
  private Application app2;
  private Policy orgPolicy;
  private Policy app1Policy;
  private PolicyEvaluation app1PolicyEvaluation;
  private PolicyEvaluation app2PolicyEvaluation;
  private PolicyViolation orgPolicyViolation;
  private PolicyViolation app1PolicyViolation;
  private PolicyViolation app2PolicyViolation;
  private Tag tag1;
  private Tag tag2;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplication("app2", "app2", org.getId());
    tempEntity.newPolicy(org.getParentOrganizationId(), "root org owned policy", 4);
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(orgPolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app1PolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app2PolicyViolation.getId(), app2.getId(), BuildStageType.ID);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
    tag1 = tempEntity.newTag(org.getId());
    tag2 = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app1.getId(), tag2.getId());
    tempEntity.newUser(USERNAME);
  }
  
  @Test
  public void testGetFilterSummary_NoFilter() throws Exception {
    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, null, null, null, null, null);
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.matchedPolicies, is(3));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByApp() throws Exception {
    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, Collections.singleton(app2.getId()), null,
        null, null, null);
    assertThat(summary.matchedApplications, is(1));
    assertThat(summary.matchedPolicies, is(2));
    assertThat(summary.matchedComponents, is(1));
  }

  @Test
  public void testGetFilterSummary_FilterByOrg() throws Exception {
    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(Collections.singleton(app2.getParentOwnerId()),
        null, null, null, null, null);
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.matchedPolicies, is(3));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, null, null,
        Collections.singleton(app2Tag.getId()), null, null);
    assertThat(summary.matchedApplications, is(1));
    assertThat(summary.matchedPolicies, is(2));
    assertThat(summary.matchedComponents, is(1));
  }

  @Test
  public void testGetFilterSummary_FilterByPolicyThreatLevel() throws Exception {
    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, null, null, null, null,
        new PolicyThreatLevelFilter(orgPolicy.getThreatLevel(), orgPolicy.getThreatLevel()));
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.matchedPolicies, is(1));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByPolicyThreatCategory() throws Exception {
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));
    orgPolicy.setConstraints(Collections.singletonList(constraint));
    new PolicyDAO().update(orgPolicy);

    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, null, null, null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE), null);
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.matchedPolicies, is(1));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByStage() throws Exception {
    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null);
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.matchedPolicies, is(3));
    assertThat(summary.matchedComponents, is(2));
  }

  @Test
  public void testDashboardFilterDefaultFilter() throws Exception {
    DashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual, notNullValue());
    assertThat(actual.minPolicyThreatLevel, is(2));
    assertThat(actual.maxPolicyThreatLevel, is(10));
    assertThat(actual.applicationFilters, hasSize(0));
    assertThat(actual.tagFilters, hasSize(0));
    assertThat(actual.policyThreatCategoryFilters, hasSize(0));
    assertThat(actual.stageTypeFilters, hasSize(0));
  }

  @Test
  public void testGetNamedDashboardFiltersForCurrentUser() throws IOException {
    String filterName1 = "Filter1";
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName1, 2, 10);
    tempEntity.newDashboardFilter(USERNAME, filterName1, JsonUtils.format(dto1.filter));

    String filterName2 = "Filter2";
    NamedDashboardFilterDTO dto2 = createNamedDashboardFilterDTO(filterName2, 3, 9);
    tempEntity.newDashboardFilter(USERNAME, filterName2, JsonUtils.format(dto2.filter));

    String filterName3 = "";
    NamedDashboardFilterDTO dto3 = createNamedDashboardFilterDTO(filterName3, 5, 9);
    tempEntity.newDashboardFilter(USERNAME, filterName3, JsonUtils.format(dto3.filter));

    List<NamedDashboardFilterDTO> actual = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(actual, hasSize(2));
    assertThat(actual.get(0).name, is(filterName1));
    assertThat(actual.get(0).filter.minPolicyThreatLevel, is(2));
    assertThat(actual.get(0).filter.maxPolicyThreatLevel, is(10));
    assertThat(actual.get(0).filter.applicationFilters, empty());
    assertThat(actual.get(0).filter.organizationFilters, empty());
    assertThat(actual.get(0).filter.tagFilters, empty());
    assertThat(actual.get(0).filter.policyThreatCategoryFilters, empty());
    assertThat(actual.get(0).filter.stageTypeFilters, empty());

    assertThat(actual.get(1).name, is(filterName2));
    assertThat(actual.get(1).filter.minPolicyThreatLevel, is(3));
    assertThat(actual.get(1).filter.maxPolicyThreatLevel, is(9));
    assertThat(actual.get(1).filter.applicationFilters, empty());
    assertThat(actual.get(1).filter.organizationFilters, empty());
    assertThat(actual.get(1).filter.tagFilters, empty());
    assertThat(actual.get(1).filter.policyThreatCategoryFilters, empty());
    assertThat(actual.get(1).filter.stageTypeFilters, empty());
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Update() throws IOException {

    String filterName1 = "Filter1";
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName1, 2, 10);
    DashboardFilter filter1 = tempEntity.newDashboardFilter(USERNAME, filterName1, JsonUtils.format(dto1.filter));

    NamedDashboardFilterDTO dto2 = createNamedDashboardFilterDTO(filterName1, 3, 9);
    //this should update the above filter
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto2);

    //verify that the filter above was updated successfully
    DashboardFilter actual = new DashboardFilterDAO().getById(filter1.getId());
    DashboardFilterDTO actualDto = JsonUtils.parse(actual.getFilter(), DashboardFilterDTO.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getUsername(), is(filter1.getUsername()));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(filter1.getNameLowercaseNoWhitespace()));
    assertThat(actual.getName(), is(filterName1));
    assertThat(actualDto.minPolicyThreatLevel, is(3));
    assertThat(actualDto.maxPolicyThreatLevel, is(9));
    assertThat(actualDto.applicationFilters, empty());
    assertThat(actualDto.organizationFilters, empty());
    assertThat(actualDto.tagFilters, empty());
    assertThat(actualDto.policyThreatCategoryFilters, empty());
    assertThat(actualDto.stageTypeFilters, empty());
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Insert() throws IOException {
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO("Filter1", 2, 10);
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto1);

    List<DashboardFilter> actual = new DashboardFilterDAO().getNamedFiltersByUsername(USERNAME);
    assertThat(actual, hasSize(1));
    DashboardFilterDTO actualDto = JsonUtils.parse(actual.get(0).getFilter(), DashboardFilterDTO.class);
    assertThat(actual.get(0).getId(), notNullValue());
    assertThat(actual.get(0).getUsername(), is(USERNAME));
    assertThat(actual.get(0).getName(), is("Filter1"));
    assertThat(actual.get(0).getNameLowercaseNoWhitespace(), is("filter1"));
    assertThat(actualDto.minPolicyThreatLevel, is(2));
    assertThat(actualDto.maxPolicyThreatLevel, is(10));
    assertThat(actualDto.applicationFilters, empty());
    assertThat(actualDto.organizationFilters, empty());
    assertThat(actualDto.tagFilters, empty());
    assertThat(actualDto.policyThreatCategoryFilters, empty());
    assertThat(actualDto.stageTypeFilters, empty());
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser() throws IOException {
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO("", 5, 7);
    tempEntity.newDashboardFilter(USERNAME, "", JsonUtils.format(dto1.filter));

    DashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual, notNullValue());
    assertThat(actual.minPolicyThreatLevel, is(5));
    assertThat(actual.maxPolicyThreatLevel, is(7));
    assertThat(actual.applicationFilters, empty());
    assertThat(actual.organizationFilters, empty());
    assertThat(actual.tagFilters, empty());
    assertThat(actual.policyThreatCategoryFilters, empty());
    assertThat(actual.stageTypeFilters, empty());
  }

  private NamedDashboardFilterDTO createNamedDashboardFilterDTO(String filterName,
                                                                int minPolicyThreatLevel,
                                                                int maxPolicyThreatLevel)
  {
    NamedDashboardFilterDTO dto = new NamedDashboardFilterDTO();
    DashboardFilterDTO filter = new DashboardFilterDTO();
    filter.applicationFilters = new ArrayList<>();
    filter.organizationFilters = new ArrayList<>();
    filter.minPolicyThreatLevel = minPolicyThreatLevel;
    filter.maxPolicyThreatLevel = maxPolicyThreatLevel;
    filter.stageTypeFilters = new ArrayList<>();
    filter.policyThreatCategoryFilters = new ArrayList<>();
    filter.tagFilters = new ArrayList<>();
    dto.name = filterName;
    dto.filter = filter;
    return dto;
  }
}
