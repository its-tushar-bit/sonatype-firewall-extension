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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardIndexDimensionQueryBuilderTest
{
  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private Configuration configuration;

  private DashboardIndexDimensionQueryBuilder builder() {
    return new DashboardIndexDimensionQueryBuilder(configuration);
  }

  // buildOrganizationFilterClausesById: one ancestor-match term per organization on
  // PARENT_ORGANIZATION_ID, so each clause selects that organization's whole subtree.

  @Test
  public void buildOrganizationFilterClausesById_singleOrgId_usesParentOrganizationIdMatch() {
    Map<String, String> clauses = builder().buildOrganizationFilterClausesById(Set.of("org-123"));

    // Ids are Lucene-escaped so a hyphen (or any query-syntax char) is matched literally.
    assertThat(clauses).containsEntry("org-123", "parentOrganizationId:org\\-123");
  }

  @Test
  public void buildOrganizationFilterClausesById_multipleOrgIds_usesParentOrganizationIdMatch() {
    Map<String, String> clauses = builder().buildOrganizationFilterClausesById(
        new LinkedHashSet<>(Set.of("org-1", "org-2", "org-3")));

    assertThat(clauses).hasSize(3);
    assertThat(clauses).containsEntry("org-1", "parentOrganizationId:org\\-1");
    assertThat(clauses).containsEntry("org-2", "parentOrganizationId:org\\-2");
    assertThat(clauses).containsEntry("org-3", "parentOrganizationId:org\\-3");
  }

  @Test
  public void buildOrganizationFilterClausesById_emptyInput_returnsEmptyMap() {
    assertThat(builder().buildOrganizationFilterClausesById(Set.of())).isEmpty();
    assertThat(builder().buildOrganizationFilterClausesById(null)).isEmpty();
  }

  @Test
  public void buildOrganizationFilterClausesById_skipsBlankAndRootOrganizationIds() {
    Map<String, String> clauses = builder().buildOrganizationFilterClausesById(
        new LinkedHashSet<>(Set.of("", "  ", Organization.ROOT_ORGANIZATION_ID, "org-ok")));

    assertThat(clauses).containsOnlyKeys("org-ok");
    assertThat(clauses).doesNotContainKey(Organization.ROOT_ORGANIZATION_ID);
  }

  // buildOrganizationFilterClause: one clause covering every requested organization's subtree.

  @Test
  public void buildOrganizationFilterClause_singleOrgId_usesParentOrganizationIdMatch() {
    String clause = builder().buildOrganizationFilterClause(Set.of("org-123"));

    assertThat(clause).isEqualTo("parentOrganizationId:org\\-123");
  }

  @Test
  public void buildOrganizationFilterClause_multipleOrgIds_usesParentOrganizationIdOr() {
    String clause = builder().buildOrganizationFilterClause(
        new LinkedHashSet<>(Set.of("org-1", "org-2", "org-3")));

    assertThat(clause).isEqualTo("parentOrganizationId:(org\\-1 org\\-2 org\\-3)");
  }

  @Test
  public void buildOrganizationFilterClause_emptyInput_returnsNull() {
    assertThat(builder().buildOrganizationFilterClause(null)).isNull();
    assertThat(builder().buildOrganizationFilterClause(Set.of())).isNull();
  }

  @Test
  public void buildOrganizationFilterClause_rootOrganizationId_returnsNull() {
    assertThat(builder().buildOrganizationFilterClause(Set.of(Organization.ROOT_ORGANIZATION_ID))).isNull();
  }

  /**
   * Selecting the root organization alongside others means the whole estate, since every organization is
   * beneath root: the filter is dropped entirely rather than narrowed to the other selections.
   */
  @Test
  public void buildOrganizationFilterClause_rootMixedWithAnotherOrg_returnsNull() {
    assertThat(builder().buildOrganizationFilterClause(
        new LinkedHashSet<>(Set.of(Organization.ROOT_ORGANIZATION_ID, "org-parent")))).isNull();
  }

  /**
   * A blank id would otherwise be escaped into a whitespace-only term that silently matches nothing.
   */
  @Test
  public void buildOrganizationFilterClause_blankIds_areDropped() {
    assertThat(builder().buildOrganizationFilterClause(new LinkedHashSet<>(Set.of(" ", "org-parent"))))
        .isEqualTo("parentOrganizationId:org\\-parent");
    assertThat(builder().buildOrganizationFilterClause(new LinkedHashSet<>(Set.of(" ", "")))).isNull();
  }

  /**
   * Ancestor-match resolves a subtree from the index, so the clause is one term regardless of hierarchy
   * depth and the builder never reads the organization hierarchy from the database to expand descendants.
   */
  @Test
  public void buildOrganizationFilterClause_matchesTheSubtreeWithoutReadingTheHierarchy() {
    String clause = builder().buildOrganizationFilterClause(Set.of("org-parent"));

    assertThat(clause).isEqualTo("parentOrganizationId:org\\-parent");
    verifyNoInteractions(organizationDAO);
  }

  // Budget-exempt term-set restrictions. The organization term set carries the caller's selection
  // against parentOrganizationId, so one id per selected organization already covers its subtree.

  @Test
  public void organizationFilterIds_selectionIsNotExpandedToDescendants() {
    Set<String> ids = builder().organizationFilterIds(Set.of("org-parent"));

    assertThat(ids).containsExactly("org-parent");
    verifyNoInteractions(organizationDAO);
  }

  @Test
  public void organizationFilterIds_rootIsUnrestricted() {
    assertThat(builder().organizationFilterIds(Set.of(Organization.ROOT_ORGANIZATION_ID))).isNull();
  }

  @Test
  public void organizationFilterIds_blankOnlyInput_returnsNoMatchSentinel() {
    assertThat(builder().organizationFilterIds(Set.of(" ")))
        .containsExactly(DashboardIndexDimensionQueryBuilder.NO_MATCH_ORGANIZATION_FILTER_ID);
  }

  @Test
  public void buildScopeFilterRestrictions_bothOrgAndApp_isClassicOrGroup() {
    List<IndexFilterRestriction> restrictions =
        builder().buildScopeFilterRestrictions(Set.of("org-a"), Set.of("app-1"));

    assertThat(restrictions).hasSize(1);
    assertThat(restrictions.get(0)).isInstanceOf(IndexOrTermSetGroup.class);
    IndexOrTermSetGroup orGroup = (IndexOrTermSetGroup) restrictions.get(0);
    assertThat(orGroup.alternatives()).hasSize(2);
    assertThat(orGroup.alternatives().get(0).field())
        .isEqualTo(FieldIdentifier.PARENT_ORGANIZATION_ID.label);
    assertThat(orGroup.alternatives().get(0).ids()).containsExactly("org-a");
    assertThat(orGroup.alternatives().get(1).field()).isEqualTo(FieldIdentifier.APPLICATION_ID.label);
    assertThat(orGroup.alternatives().get(1).ids()).containsExactly("app-1");
    verifyNoInteractions(organizationDAO);
  }

  @Test
  public void buildScopeFilterRestrictionsAnd_bothOrgAndApp_areSeparateTermSets() {
    List<IndexFilterRestriction> restrictions =
        builder().buildScopeFilterRestrictionsAnd(Set.of("org-a"), Set.of("app-1"));

    assertThat(restrictions).hasSize(2);
    assertThat(((IndexTermSetRestriction) restrictions.get(0)).field())
        .isEqualTo(FieldIdentifier.PARENT_ORGANIZATION_ID.label);
    assertThat(((IndexTermSetRestriction) restrictions.get(0)).ids()).containsExactly("org-a");
    assertThat(((IndexTermSetRestriction) restrictions.get(1)).field())
        .isEqualTo(FieldIdentifier.APPLICATION_ID.label);
    assertThat(((IndexTermSetRestriction) restrictions.get(1)).ids()).containsExactly("app-1");
    verifyNoInteractions(organizationDAO);
  }

  @Test
  public void buildEscapedApplicationFilterClause_rejectsTooManyIdsForStringCallers() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);

    assertThatThrownBy(() -> builder().buildEscapedApplicationFilterClause(Set.of("a", "b", "c")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many ids");
  }
}
