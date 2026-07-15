/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationsListViolationQuerySupportTest
{
  @Mock
  private Configuration configuration;

  @Mock
  private ApplicationsListViolationScopeResolver violationScopeResolver;

  @Test
  public void buildApplicationQuery_alwaysStartsWithApplicationItemType() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.search = "apple";
    String query = new ApplicationsListIndexQueryBuilder(
        new com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder(null, configuration),
        violationScopeResolver)
            .buildApplicationQuery(request);

    assertThat(query).startsWith("itemType:APPLICATION");
  }

  @Test
  public void buildThreatFilterClause_disjointCriticalAndLow_doesNotMatchSevere() {
    List<PolicyThreatLevelFilter> threatFilters = List.of(
        new PolicyThreatLevelFilter(8, 10),
        new PolicyThreatLevelFilter(1, 1));

    String clause = ApplicationsListViolationQuerySupport.buildThreatFilterClause(threatFilters);

    assertThat(clause).isEqualTo(
        "(policyViolationThreatLevel:[8 TO 10] OR policyViolationThreatLevel:[1 TO 1])");
    assertThat(clause).doesNotContain("[1 TO 10]");
    assertThat(clause).doesNotContain("[4 TO 7]");
  }

  @Test
  public void toViolationQuery_requiresApplicationItemTypePrefix() {
    String violationQuery = ApplicationsListViolationQuerySupport.toViolationQuery(
        "itemType:APPLICATION AND applicationName:*foo*");

    assertThat(violationQuery).startsWith("itemType:POLICY_VIOLATION");
    assertThat(violationQuery).contains("applicationName:*foo*");
  }

  @Test
  public void toViolationQuery_rejectsMissingApplicationItemTypePrefix() {
    assertThatThrownBy(() -> ApplicationsListViolationQuerySupport.toViolationQuery("applicationName:*foo*"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("itemType:APPLICATION");
  }

  @Test
  public void threatLevelFilterForCardEnrichment_mapsPluralRangesToOrFilter() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatLevelRanges = List.of(
        new PolicyThreatLevelFilter(8, 10),
        new PolicyThreatLevelFilter(1, 1));

    PolicyThreatLevelFilter enrichment =
        ApplicationsListViolationQuerySupport.threatLevelFilterForCardEnrichment(request);

    assertThat(enrichment).isInstanceOf(PolicyThreatLevelOrFilter.class);
    assertThat(enrichment.test(10)).isTrue();
    assertThat(enrichment.test(1)).isTrue();
    assertThat(enrichment.test(5)).isFalse();
  }

  @Test
  public void hasViolationScopedFilters_ignoresNoOpThreatRanges() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatLevelRanges = List.of(new PolicyThreatLevelFilter(Integer.MIN_VALUE, Integer.MAX_VALUE));

    assertThat(ApplicationsListViolationQuerySupport.hasViolationScopedFilters(request)).isFalse();
  }

  @Test
  public void policyThreatLevelOrFilter_rejectsEmptyRanges() {
    assertThatThrownBy(() -> new PolicyThreatLevelOrFilter(List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one threat range");
  }

  @Test
  public void policyThreatLevelOrFilter_rejectsNullRangeElement() {
    List<PolicyThreatLevelFilter> ranges = new ArrayList<>();
    ranges.add(new PolicyThreatLevelFilter(8, 10));
    ranges.add(null);

    assertThatThrownBy(() -> new PolicyThreatLevelOrFilter(ranges))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null elements");
  }
}
