/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DashboardIndexDimensionQueryBuilderTest
{
  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private Configuration configuration;

  private DashboardIndexDimensionQueryBuilder builder() {
    return new DashboardIndexDimensionQueryBuilder(organizationDAO, configuration);
  }

  @Test
  public void buildOrganizationFilterClausesById_softSkipsOversizedOrgAndKeepsOthers() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    Set<String> huge = IntStream.rangeClosed(1, 3)
        .mapToObj(i -> "child-" + i)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    when(organizationDAO.getChildOrganizationIdsGroupedByAncestor(any()))
        .thenReturn(Map.of(
            "org-huge", huge,
            "org-ok", Set.of("org-ok", "org-ok-child")));

    Map<String, String> clauses =
        builder().buildOrganizationFilterClausesById(Set.of("org-huge", "org-ok"));

    assertThat(clauses).doesNotContainKey("org-huge");
    assertThat(clauses).containsEntry("org-ok", "organizationId:(org-ok org-ok-child)");
  }

  @Test
  public void buildOrganizationFilterClause_stillRejectsOversizedExpansion() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    when(organizationDAO.getAllChildOrganizationIds(any()))
        .thenReturn(Set.of("a", "b", "c"));

    assertThatThrownBy(() -> builder().buildOrganizationFilterClause(Set.of("org-huge")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many organizations");
  }

  @Test
  public void buildOrganizationFilterClausesById_missingAncestorRows_emitNoMatchSentinel() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(organizationDAO.getChildOrganizationIdsGroupedByAncestor(any()))
        .thenReturn(Map.of());

    Map<String, String> clauses = builder().buildOrganizationFilterClausesById(Set.of("org-orphan"));

    assertThat(clauses).containsEntry(
        "org-orphan",
        "organizationId:(" + DashboardIndexDimensionQueryBuilder.NO_MATCH_ORGANIZATION_FILTER_ID + ")");
  }

  @Test
  public void buildOrganizationFilterClausesById_emptyInput_returnsEmptyMap() {
    assertThat(builder().buildOrganizationFilterClausesById(Set.of())).isEmpty();
    assertThat(builder().buildOrganizationFilterClausesById(null)).isEmpty();
  }

  @Test
  public void buildOrganizationFilterClausesById_keepsOrgAtExactMaxClauseBoundary() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(3);
    Set<String> exactMax = IntStream.rangeClosed(1, 3)
        .mapToObj(i -> "child-" + i)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    when(organizationDAO.getChildOrganizationIdsGroupedByAncestor(any()))
        .thenReturn(Map.of("org-exact", exactMax));

    Map<String, String> clauses = builder().buildOrganizationFilterClausesById(Set.of("org-exact"));

    assertThat(clauses).containsKey("org-exact");
    assertThat(clauses.get("org-exact")).startsWith("organizationId:(");
  }

  @Test
  public void buildOrganizationFilterClausesById_emptyDescendants_emitNoMatchSentinel() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(organizationDAO.getChildOrganizationIdsGroupedByAncestor(any()))
        .thenReturn(Map.of("org-empty", Set.of()));

    Map<String, String> clauses = builder().buildOrganizationFilterClausesById(Set.of("org-empty"));

    assertThat(clauses).containsEntry(
        "org-empty",
        "organizationId:(" + DashboardIndexDimensionQueryBuilder.NO_MATCH_ORGANIZATION_FILTER_ID + ")");
  }

  @Test
  public void buildOrganizationFilterClausesById_skipsBlankAndRootOrganizationIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);
    when(organizationDAO.getChildOrganizationIdsGroupedByAncestor(any()))
        .thenReturn(Map.of("org-ok", Set.of("org-ok")));

    Map<String, String> clauses = builder().buildOrganizationFilterClausesById(
        new LinkedHashSet<>(Set.of("", "  ", Organization.ROOT_ORGANIZATION_ID, "org-ok")));

    assertThat(clauses).containsOnlyKeys("org-ok");
    assertThat(clauses).doesNotContainKey(Organization.ROOT_ORGANIZATION_ID);
  }
}
