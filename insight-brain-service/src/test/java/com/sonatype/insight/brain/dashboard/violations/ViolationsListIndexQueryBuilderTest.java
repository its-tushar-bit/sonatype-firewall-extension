/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Violations list Lucene query construction — the direct-coverage counterpart to
 * {@code ApplicationsListIndexQueryBuilderTest}, so query shape is asserted here rather than only
 * indirectly through {@code ViolationsListResourceTest} (CLM-42254 review).
 */
@RunWith(MockitoJUnitRunner.class)
public class ViolationsListIndexQueryBuilderTest
{
  private static final String BASE = "itemType:POLICY_VIOLATION";

  @Mock
  private Configuration configuration;

  @Mock
  private OrganizationDAO organizationDAO;

  private ViolationsListIndexQueryBuilder newBuilder() {
    return new ViolationsListIndexQueryBuilder(
        new DashboardIndexDimensionQueryBuilder(organizationDAO, configuration));
  }

  @Test
  public void buildViolationQuery_nullOrBlankSearch_returnsItemTypeOnly() {
    assertThat(newBuilder().buildViolationQuery(null)).isEqualTo(BASE);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(BASE);

    request.search = "   ";
    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(BASE);
  }

  @Test
  public void buildViolationQuery_singleTerm_matchesAllFiveSearchFields() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.search = "apple";

    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(
        BASE + " AND (componentName:*apple* OR applicationName:*apple* OR applicationPublicId:*apple*"
            + " OR organizationName:*apple* OR policyViolationPolicyName:*apple*)");
  }

  @Test
  public void buildViolationQuery_multiWordSearch_andsTokensAcrossFields() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.search = "apple pie";

    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(
        BASE + " AND ((componentName:*apple* OR applicationName:*apple* OR applicationPublicId:*apple*"
            + " OR organizationName:*apple* OR policyViolationPolicyName:*apple*)"
            + " AND (componentName:*pie* OR applicationName:*pie* OR applicationPublicId:*pie*"
            + " OR organizationName:*pie* OR policyViolationPolicyName:*pie*))");
  }

  @Test
  public void buildViolationQuery_threatLevelRange_bothBounds() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyThreatLevelRange = new PolicyThreatLevelFilter(7, 10);

    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND policyViolationThreatLevel:[7 TO 10]");
  }

  @Test
  public void buildViolationQuery_threatLevelRange_minOnly_clampsMaxToTen() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyThreatLevelRange = new PolicyThreatLevelFilter(7, null);

    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND policyViolationThreatLevel:[7 TO 10]");
  }

  @Test
  public void buildViolationQuery_threatLevelRange_maxOnly_clampsMinToZero() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyThreatLevelRange = new PolicyThreatLevelFilter(null, 5);

    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND policyViolationThreatLevel:[0 TO 5]");
  }

  @Test
  public void buildViolationQuery_threatLevelRange_noBounds_omitsClause() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyThreatLevelRange = new PolicyThreatLevelFilter((Integer) null, (Integer) null);

    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(BASE);
  }

  @Test
  public void buildViolationQuery_threatCategory_matchesCategoryName() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyThreatCategories = new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY);

    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND policyViolationThreatCategory:(" + PolicyThreatCategory.SECURITY.getName() + ")");
  }

  @Test
  public void buildViolationQuery_openState_matchesNotWaived() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.OPEN);

    // OPEN is expressed as the complement of the excluded set (Waived AutoWaived Legacy) so the filter
    // agrees with the OPEN facet count and the row-state derivation — a violation with an absent/unknown
    // waiver status is OPEN on all three paths, and Legacy is excluded from OPEN on all three.
    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND NOT (policyViolationWaiverStatus:(Waived AutoWaived Legacy))");
  }

  @Test
  public void buildViolationQuery_legacyState_matchesLegacyOnly() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.LEGACY_VIOLATION);

    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND policyViolationWaiverStatus:(Legacy)");
  }

  @Test
  public void buildViolationQuery_openAndLegacyStates_orsAnchoredOpenWithLegacy() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyViolationStates =
        new PolicyViolationStateFilter(PolicyViolationState.OPEN, PolicyViolationState.LEGACY_VIOLATION);

    // OR-combined OPEN carries its own *:* positive anchor so the negation is not left as a
    // pure-negative SHOULD (which would zero out the whole OR against a real index).
    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(
        BASE + " AND ((*:* AND NOT (policyViolationWaiverStatus:(Waived AutoWaived Legacy)))"
            + " OR policyViolationWaiverStatus:(Legacy))");
  }

  @Test
  public void buildViolationQuery_waivedAndLegacyStates_orsClauses() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyViolationStates =
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.LEGACY_VIOLATION);

    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(
        BASE + " AND (policyViolationWaiverStatus:(Waived AutoWaived)"
            + " OR policyViolationWaiverStatus:(Legacy))");
  }

  @Test
  public void buildViolationQuery_allThreeStates_omitsStateClause() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyViolationStates = new PolicyViolationStateFilter(
        PolicyViolationState.OPEN, PolicyViolationState.WAIVED, PolicyViolationState.LEGACY_VIOLATION);

    // OPEN OR WAIVED OR LEGACY is the whole indexed domain, so no state clause is emitted.
    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(BASE);
  }

  @Test
  public void buildViolationQuery_waivedState_matchesWaivedAndAutoWaived() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.WAIVED);

    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND policyViolationWaiverStatus:(Waived AutoWaived)");
  }

  @Test
  public void buildViolationQuery_openAndWaivedStates_orsAnchoredOpenWithWaived() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyViolationStates =
        new PolicyViolationStateFilter(PolicyViolationState.OPEN, PolicyViolationState.WAIVED);

    // OPEN (excludes Waived/AutoWaived/Legacy) OR WAIVED covers everything except the pure-legacy
    // population, so a state clause IS emitted (unlike selecting all three states). OR-combined OPEN
    // carries its own *:* positive anchor so the negation is not left as a pure-negative SHOULD.
    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(
        BASE + " AND ((*:* AND NOT (policyViolationWaiverStatus:(Waived AutoWaived Legacy)))"
            + " OR policyViolationWaiverStatus:(Waived AutoWaived))");
  }

  @Test
  public void buildViolationQuery_autoWaiverFilter_matchesAutoWaivedOnly() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.waivedWithAutoWaiver = Boolean.TRUE;

    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND policyViolationWaiverStatus:(AutoWaived)");
  }

  @Test
  public void buildViolationQuery_manualWaiverFilter_matchesWaivedOnly() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.waivedWithAutoWaiver = Boolean.FALSE;

    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND policyViolationWaiverStatus:(Waived)");
  }

  @Test
  public void buildViolationQuery_nullWaiverFilter_omitsClause() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.waivedWithAutoWaiver = null;

    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(BASE);
  }

  @Test
  public void buildViolationQueryExcludingWaiverType_dropsWaiverClauseButKeepsOtherFilters() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.waivedWithAutoWaiver = Boolean.TRUE;
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.WAIVED);

    // The waiver-excluded query (used to count the single-select waiver-type facet) drops only the
    // waiver-type clause; every other active filter — here the WAIVED state clause — is retained.
    assertThat(newBuilder().buildViolationQueryExcludingWaiverType(request))
        .isEqualTo(BASE + " AND policyViolationWaiverStatus:(Waived AutoWaived)");
  }

  @Test
  public void buildViolationQueryExcludingWaiverType_matchesFullQueryWhenNoWaiverFilter() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.stageIds = Set.of("build");

    // With no waiver-type filter active the two queries are identical, so the facet counts land against
    // the same query as the list rows.
    assertThat(newBuilder().buildViolationQueryExcludingWaiverType(request))
        .isEqualTo(newBuilder().buildViolationQuery(request));
  }

  @Test
  public void buildViolationQuery_openStateWithAutoWaiver_andsToContradictoryClauses() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.OPEN);
    request.waivedWithAutoWaiver = Boolean.TRUE;

    // OPEN ("not waived/legacy") AND auto-waived is intentionally contradictory — the index returns no
    // rows, which is the correct result for that filter combination.
    assertThat(newBuilder().buildViolationQuery(request)).isEqualTo(
        BASE + " AND NOT (policyViolationWaiverStatus:(Waived AutoWaived Legacy))"
            + " AND policyViolationWaiverStatus:(AutoWaived)");
  }

  @Test
  public void buildViolationQuery_stageFilter_matchesStageIds() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.stageIds = Set.of("build");

    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND policyEvaluationStage:(build)");
  }

  @Test
  public void buildViolationQuery_rejectsBlankStageId() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    Set<String> stageIds = new LinkedHashSet<>();
    stageIds.add("build");
    stageIds.add("  ");
    request.stageIds = stageIds;

    assertThatThrownBy(() -> newBuilder().buildViolationQuery(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("stageIds");
  }

  @Test
  public void buildViolationQuery_rootOrgWithApplicationFilter_appFilterTakesPrecedence() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(10);

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.organizationIds = Set.of(Organization.ROOT_ORGANIZATION_ID);
    request.applicationIds = Set.of("appx");

    // Root org yields a null org clause ("all orgs"); combined with an explicit app filter the query
    // narrows to the app rather than widening back to all orgs (documented precedence, CLM-42254).
    assertThat(newBuilder().buildViolationQuery(request))
        .isEqualTo(BASE + " AND (applicationId:(appx))");
  }

  @Test
  public void buildViolationQuery_rejectsTooManyApplicationIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);

    Set<String> applicationIds = new LinkedHashSet<>();
    IntStream.range(0, 3).forEach(i -> applicationIds.add("app-" + i));
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.applicationIds = applicationIds;

    assertThatThrownBy(() -> newBuilder().buildViolationQuery(request))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void buildViolationQuery_combinesFiltersWithAnd() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.search = "log4j";
    request.policyThreatLevelRange = new PolicyThreatLevelFilter(7, 10);
    request.policyThreatCategories = new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY);
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.OPEN);

    String query = newBuilder().buildViolationQuery(request);

    assertThat(query).startsWith(BASE + " AND ");
    assertThat(query).contains("componentName:*log4j*");
    assertThat(query).contains("policyViolationThreatLevel:[7 TO 10]");
    assertThat(query).contains("policyViolationThreatCategory:(" + PolicyThreatCategory.SECURITY.getName() + ")");
    assertThat(query).contains("NOT (policyViolationWaiverStatus:(Waived AutoWaived Legacy))");
  }
}
