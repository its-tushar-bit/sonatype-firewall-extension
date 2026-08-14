/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexOrTermSetGroup;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
  public void expandOrganizationFilterIds_doesNotRejectOversizedExpansion() {
    when(organizationDAO.getAllChildOrganizationIds(any()))
        .thenReturn(Set.of("a", "b", "c"));

    assertThat(builder().expandOrganizationFilterIds(Set.of("org-huge")))
        .containsExactlyInAnyOrder("a", "b", "c");
  }

  @Test
  public void buildOrganizationFilterClause_rejectsOversizedExpansionForStringCallers() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    when(organizationDAO.getAllChildOrganizationIds(any()))
        .thenReturn(Set.of("a", "b", "c"));

    assertThatThrownBy(() -> builder().buildOrganizationFilterClause(Set.of("org-huge")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many organizations");
  }

  @Test
  public void buildEscapedApplicationFilterClause_rejectsTooManyIdsForStringCallers() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);

    assertThatThrownBy(() -> builder().buildEscapedApplicationFilterClause(Set.of("a", "b", "c")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many ids");
  }

  @Test
  public void buildScopeFilterRestrictions_bothOrgAndApp_isClassicOrGroup() {
    when(organizationDAO.getAllChildOrganizationIds(any())).thenReturn(Set.of("org-a", "org-a-child"));

    List<IndexFilterRestriction> restrictions =
        builder().buildScopeFilterRestrictions(Set.of("org-a"), Set.of("app-1"));

    assertThat(restrictions).hasSize(1);
    assertThat(restrictions.get(0)).isInstanceOf(IndexOrTermSetGroup.class);
    IndexOrTermSetGroup orGroup = (IndexOrTermSetGroup) restrictions.get(0);
    assertThat(orGroup.alternatives()).hasSize(2);
    assertThat(orGroup.alternatives().get(0).field()).isEqualTo(FieldIdentifier.ORGANIZATION_ID.label);
    assertThat(orGroup.alternatives().get(0).ids()).containsExactlyInAnyOrder("org-a", "org-a-child");
    assertThat(orGroup.alternatives().get(1).field()).isEqualTo(FieldIdentifier.APPLICATION_ID.label);
    assertThat(orGroup.alternatives().get(1).ids()).containsExactly("app-1");
  }

  @Test
  public void buildScopeFilterRestrictionsAnd_bothOrgAndApp_areSeparateTermSets() {
    when(organizationDAO.getAllChildOrganizationIds(any())).thenReturn(Set.of("org-a"));

    List<IndexFilterRestriction> restrictions =
        builder().buildScopeFilterRestrictionsAnd(Set.of("org-a"), Set.of("app-1"));

    assertThat(restrictions).hasSize(2);
    assertThat(restrictions.get(0)).isInstanceOf(IndexTermSetRestriction.class);
    assertThat(((IndexTermSetRestriction) restrictions.get(0)).field())
        .isEqualTo(FieldIdentifier.ORGANIZATION_ID.label);
    assertThat(((IndexTermSetRestriction) restrictions.get(0)).ids()).containsExactly("org-a");
    assertThat(restrictions.get(1)).isInstanceOf(IndexTermSetRestriction.class);
    assertThat(((IndexTermSetRestriction) restrictions.get(1)).field())
        .isEqualTo(FieldIdentifier.APPLICATION_ID.label);
    assertThat(((IndexTermSetRestriction) restrictions.get(1)).ids()).containsExactly("app-1");
  }

  @Test
  public void expandOrganizationFilterIds_rootIsUnrestricted() {
    assertThat(builder().expandOrganizationFilterIds(Set.of(Organization.ROOT_ORGANIZATION_ID))).isNull();
  }

  @Test
  public void expandOrganizationFilterIds_emptyExpansion_returnsNoMatchSentinel() {
    when(organizationDAO.getAllChildOrganizationIds(any())).thenReturn(Set.of());

    assertThat(builder().expandOrganizationFilterIds(Set.of("org-orphan")))
        .containsExactly(DashboardIndexDimensionQueryBuilder.NO_MATCH_ORGANIZATION_FILTER_ID);
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
