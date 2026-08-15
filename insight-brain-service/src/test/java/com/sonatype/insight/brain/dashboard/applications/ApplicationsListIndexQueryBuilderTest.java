/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApplicationsListIndexQueryBuilderTest
{
  @Mock
  private Configuration configuration;

  @Mock
  private ApplicationsListViolationScopeResolver violationScopeResolver;

  private ApplicationsListIndexQueryBuilder newBuilder() {
    return new ApplicationsListIndexQueryBuilder(
        new DashboardIndexDimensionQueryBuilder(configuration),
        violationScopeResolver);
  }

  @Test
  public void buildApplicationQuery_allowsManyApplicationIdsWithoutClauseBudget() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.applicationIds = Set.of("app-a", "app-b", "app-c");

    ApplicationsListIndexQueryBuilder builder = newBuilder();
    ApplicationsIndexQuery indexQuery = builder.buildApplicationIndexQuery(request);

    assertThat(indexQuery.query()).isEqualTo("itemType:APPLICATION");
    assertThat(indexQuery.query()).doesNotContain("applicationId:(");
    IndexTermSetRestriction apps = (IndexTermSetRestriction) indexQuery.termSets().get(0);
    assertThat(apps.field()).isEqualTo(FieldIdentifier.APPLICATION_ID.label);
    assertThat(apps.ids()).containsExactlyInAnyOrder("app-a", "app-b", "app-c");
  }

  @Test
  public void buildApplicationQuery_allowsApplicationIdsAboveFormerClauseLimit() {
    Set<String> applicationIds = new LinkedHashSet<>();
    IntStream.range(0, 101).forEach(i -> applicationIds.add("app-" + i));

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.applicationIds = applicationIds;

    assertThatCode(() -> newBuilder().buildApplicationQuery(request)).doesNotThrowAnyException();
  }

  @Test
  public void buildApplicationQuery_blankSearch_returnsApplicationItemTypeOnly() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
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
  public void buildApplicationQuery_ageInDays_addsLastEvaluationRange() {
    long before = System.currentTimeMillis();
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.ageInDays = 30;

    String query = newBuilder().buildApplicationQuery(request);
    long after = System.currentTimeMillis();

    assertThat(query).startsWith("itemType:APPLICATION AND applicationLastEvaluationTimeEpochMs:[");
    String range = query.substring(query.indexOf('[') + 1, query.indexOf(']'));
    String[] bounds = range.split(" TO ");
    long lower = Long.parseLong(bounds[0]);
    long upper = Long.parseLong(bounds[1]);
    assertThat(lower).isBetween(
        before - TimeUnit.DAYS.toMillis(30),
        after - TimeUnit.DAYS.toMillis(30));
    assertThat(upper).isBetween(before, after);
  }

  @Test
  public void buildApplicationQuery_stageAndAge_keepsAgeOffViolationDiscoveryQuery() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of("org-a");
    request.stageIds = Set.of("build");
    request.ageInDays = 30;
    ArgumentCaptor<String> discoveryBaseQuery = ArgumentCaptor.forClass(String.class);
    when(violationScopeResolver.resolveApplicationIds(discoveryBaseQuery.capture(), anyList(), same(request)))
        .thenReturn(Set.of("build-app"));

    ApplicationsIndexQuery indexQuery = newBuilder().buildApplicationIndexQuery(request);

    assertThat(discoveryBaseQuery.getValue()).doesNotContain("applicationLastEvaluationTimeEpochMs");
    assertThat(indexQuery.query()).contains("applicationLastEvaluationTimeEpochMs:[");
    assertThat(indexQuery.query()).doesNotContain("applicationId:(");
    IndexTermSetRestriction apps = (IndexTermSetRestriction) indexQuery.termSets().get(0);
    assertThat(apps.field()).isEqualTo(FieldIdentifier.APPLICATION_ID.label);
    assertThat(apps.ids()).containsExactly("build-app");
    verify(violationScopeResolver).resolveApplicationIds(any(), anyList(), same(request));
  }

  @Test
  public void buildApplicationQuery_rejectsNullApplicationId() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    Set<String> applicationIds = new java.util.HashSet<>();
    applicationIds.add("app-a");
    applicationIds.add(null);
    request.applicationIds = applicationIds;

    assertThatThrownBy(() -> newBuilder().buildApplicationQuery(request))
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
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of("org-a");
    request.stageIds = Set.of("build");
    when(violationScopeResolver.resolveApplicationIds(any(), anyList(), same(request)))
        .thenReturn(Set.of("build-app"));

    ApplicationsIndexQuery indexQuery = newBuilder().buildApplicationIndexQuery(request);

    assertThat(indexQuery.query()).isEqualTo("itemType:APPLICATION");
    assertThat(indexQuery.query()).doesNotContain("organizationId:");
    IndexTermSetRestriction apps = (IndexTermSetRestriction) indexQuery.termSets().get(0);
    assertThat(apps.field()).isEqualTo(FieldIdentifier.APPLICATION_ID.label);
    assertThat(apps.ids()).containsExactly("build-app");
  }

  @Test
  public void buildApplicationQuery_threatFilter_putsScopedAppsInTermSetsNotString() {
    PolicyThreatLevelFilter threatFilter = new PolicyThreatLevelFilter(8, 10);
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of("org-a");
    request.policyThreatLevelRanges = List.of(threatFilter);
    when(violationScopeResolver.resolveApplicationIds(any(), anyList(), same(request)))
        .thenReturn(Set.of("critical-app"));

    ApplicationsIndexQuery indexQuery = newBuilder().buildApplicationIndexQuery(request);

    assertThat(indexQuery.query()).isEqualTo("itemType:APPLICATION");
    IndexTermSetRestriction apps = (IndexTermSetRestriction) indexQuery.termSets().get(0);
    assertThat(apps.ids()).containsExactly("critical-app");
  }

  @Test
  public void buildApplicationQuery_orgAndApplicationAndStageFilter_keepsAllViolationScopedOrgApps() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of("org-a");
    request.applicationIds = Set.of("app-b");
    request.stageIds = Set.of("build");
    when(violationScopeResolver.resolveApplicationIds(any(), anyList(), same(request)))
        .thenReturn(Set.of("org-app-1", "org-app-2", "app-b"));

    ApplicationsIndexQuery indexQuery = newBuilder().buildApplicationIndexQuery(request);

    assertThat(indexQuery.query()).isEqualTo("itemType:APPLICATION");
    assertThat(indexQuery.query()).doesNotContain("organizationId:");
    IndexTermSetRestriction apps = (IndexTermSetRestriction) indexQuery.termSets().get(0);
    assertThat(apps.ids()).containsExactlyInAnyOrder("org-app-1", "org-app-2", "app-b");
  }

  @Test
  public void buildApplicationQuery_policyTypeAndStateFilter_usesViolationScopedApplicationIdsOnly() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatCategories = new PolicyThreatCategoryFilter(Set.of(PolicyThreatCategory.SECURITY));
    request.policyViolationStates = new PolicyViolationStateFilter(Set.of(PolicyViolationState.OPEN));
    when(violationScopeResolver.resolveApplicationIds(any(), anyList(), same(request)))
        .thenReturn(Set.of("security-open-app"));

    ApplicationsIndexQuery indexQuery = newBuilder().buildApplicationIndexQuery(request);

    assertThat(indexQuery.query()).isEqualTo("itemType:APPLICATION");
    IndexTermSetRestriction apps = (IndexTermSetRestriction) indexQuery.termSets().get(0);
    assertThat(apps.ids()).containsExactly("security-open-app");
  }
}
