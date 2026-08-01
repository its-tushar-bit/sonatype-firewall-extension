/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationsListIndexQueryBuilderTest
{
  @Mock
  private Configuration configuration;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private ApplicationsListViolationScopeResolver violationScopeResolver;

  private ApplicationsListIndexQueryBuilder newBuilder() {
    return new ApplicationsListIndexQueryBuilder(
        new DashboardIndexDimensionQueryBuilder(organizationDAO, configuration),
        violationScopeResolver);
  }

  @Test
  public void buildApplicationQuery_rejectsTooManyApplicationIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.applicationIds = Set.of("app-a", "app-b", "app-c");

    ApplicationsListIndexQueryBuilder builder = newBuilder();

    assertThatThrownBy(() -> builder.buildApplicationQuery(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many ids");
  }

  @Test
  public void buildApplicationQuery_allowsApplicationIdsWithinClauseLimit() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(3);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.applicationIds = Set.of("app-a", "app-b");

    ApplicationsListIndexQueryBuilder builder = newBuilder();

    assertThatCode(() -> builder.buildApplicationQuery(request)).doesNotThrowAnyException();
  }

  @Test
  public void buildApplicationQuery_rejectsApplicationIdsAboveConfiguredMax() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);

    Set<String> applicationIds = new LinkedHashSet<>();
    IntStream.range(0, 101).forEach(i -> applicationIds.add("app-" + i));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.applicationIds = applicationIds;

    ApplicationsListIndexQueryBuilder builder = newBuilder();

    assertThatThrownBy(() -> builder.buildApplicationQuery(request))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void buildApplicationQuery_blankSearch_returnsApplicationItemTypeOnly() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    ApplicationsListIndexQueryBuilder builder = newBuilder();

    assertThat(newBuilder().buildApplicationQuery(request)).isEqualTo("itemType:APPLICATION");
    request.search = "   ";
    assertThat(newBuilder().buildApplicationQuery(request)).isEqualTo("itemType:APPLICATION");
  }

  @Test
  public void buildApplicationQuery_singleTerm_matchesNamePublicIdAndOrganization() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.search = "apple";
    String query = newBuilder().buildApplicationQuery(request);
    assertThat(query).isEqualTo(
        "itemType:APPLICATION AND (applicationName:*apple* OR applicationPublicId:*apple* OR organizationName:*apple*)");
  }

  @Test
  public void buildApplicationQuery_rejectsNullApplicationId() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    Set<String> applicationIds = new java.util.HashSet<>();
    applicationIds.add("app-a");
    applicationIds.add(null);
    request.applicationIds = applicationIds;

    ApplicationsListIndexQueryBuilder builder = newBuilder();

    assertThatThrownBy(() -> builder.buildApplicationQuery(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("applicationIds");
  }

  @Test
  public void buildApplicationQuery_multiWordSearch_andTokensAcrossFields() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.search = "apple pie";
    String query = newBuilder().buildApplicationQuery(request);
    assertThat(query).isEqualTo(
        "itemType:APPLICATION AND ((applicationName:*apple* OR applicationPublicId:*apple* OR organizationName:*apple*) AND (applicationName:*pie* OR applicationPublicId:*pie* OR organizationName:*pie*))");
  }

  @Test
  public void buildApplicationQuery_stageFilter_usesViolationScopedApplicationIdsOnly() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(organizationDAO.getAllChildOrganizationIds(Set.of("org-a"))).thenReturn(Set.of("org-a"));
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of("org-a");
    request.stageIds = Set.of("build");
    when(violationScopeResolver.resolveApplicationIds(any(), same(request)))
        .thenReturn(Set.of("build-app"));

    String query = newBuilder().buildApplicationQuery(request);

    assertThat(query).isEqualTo("itemType:APPLICATION AND (applicationId:(build\\-app))");
    assertThat(query).doesNotContain("organizationId:");
  }

  @Test
  public void buildApplicationQuery_threatFilter_usesViolationScopedApplicationIdsOnly() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(organizationDAO.getAllChildOrganizationIds(Set.of("org-a"))).thenReturn(Set.of("org-a"));
    PolicyThreatLevelFilter threatFilter = new PolicyThreatLevelFilter(8, 10);
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of("org-a");
    request.policyThreatLevelRanges = List.of(threatFilter);
    when(violationScopeResolver.resolveApplicationIds(any(), same(request)))
        .thenReturn(Set.of("critical-app"));

    String query = newBuilder().buildApplicationQuery(request);

    assertThat(query).isEqualTo("itemType:APPLICATION AND (applicationId:(critical\\-app))");
    assertThat(query).doesNotContain("organizationId:");
  }

  @Test
  public void buildApplicationQuery_orgAndApplicationAndStageFilter_keepsAllViolationScopedOrgApps() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(organizationDAO.getAllChildOrganizationIds(Set.of("org-a"))).thenReturn(Set.of("org-a"));
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of("org-a");
    request.applicationIds = Set.of("app-b");
    request.stageIds = Set.of("build");
    when(violationScopeResolver.resolveApplicationIds(any(), same(request)))
        .thenReturn(Set.of("org-app-1", "org-app-2", "app-b"));

    String query = newBuilder().buildApplicationQuery(request);

    assertThat(query).contains("org\\-app\\-1");
    assertThat(query).contains("org\\-app\\-2");
    assertThat(query).contains("app\\-b");
    assertThat(query).doesNotContain("organizationId:");
  }

  @Test
  public void buildApplicationQuery_policyTypeAndStateFilter_usesViolationScopedApplicationIdsOnly() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatCategories = new PolicyThreatCategoryFilter(Set.of(PolicyThreatCategory.SECURITY));
    request.policyViolationStates = new PolicyViolationStateFilter(Set.of(PolicyViolationState.OPEN));
    when(violationScopeResolver.resolveApplicationIds(any(), same(request)))
        .thenReturn(Set.of("security-open-app"));

    String query = newBuilder().buildApplicationQuery(request);

    assertThat(query).isEqualTo("itemType:APPLICATION AND (applicationId:(security\\-open\\-app))");
  }
}
