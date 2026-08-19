/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
import com.sonatype.insight.brain.search.index.ItemType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComponentsListIndexQueryBuilderTest
{
  @Mock
  private DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  @Mock
  private ComponentsListViolationScopeResolver violationScopeResolver;

  private ComponentsListIndexQueryBuilder queryBuilder;

  @BeforeEach
  public void setUp() {
    queryBuilder = new ComponentsListIndexQueryBuilder(dimensionQueryBuilder, violationScopeResolver);
  }

  @Test
  public void buildComponentQuery_includesComponentItemTypes() {
    when(dimensionQueryBuilder.buildScopeFilterRestrictions(any(), any())).thenReturn(List.of());

    String query = queryBuilder.buildComponentQuery(new ComponentsListRequestDTO());

    assertThat(query).contains(ItemType.NON_VULNERABLE_COMPONENT.name());
    assertThat(query).contains(ItemType.SECURITY_VULNERABILITY.name());
  }

  @Test
  public void buildComponentQuery_includesSearchAcrossComponentFields() {
    when(dimensionQueryBuilder.buildScopeFilterRestrictions(any(), any())).thenReturn(List.of());

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.search = "log4j";

    String query = queryBuilder.buildComponentQuery(request);

    assertThat(query).contains("componentName:");
    assertThat(query).contains("componentHash:");
    assertThat(query).contains("log4j");
  }

  @Test
  public void buildComponentQuery_appliesOrganizationFilter() {
    when(dimensionQueryBuilder.buildScopeFilterRestrictions(Set.of("org-1"), null))
        .thenReturn(IndexTermSetRestriction.singleton(FieldIdentifier.ORGANIZATION_ID.label, Set.of("org-1")));

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.organizationIds = Set.of("org-1");

    ComponentsIndexQuery result = queryBuilder.buildComponentIndexQuery(request);

    assertThat(result.query()).doesNotContain("organizationId:");
    assertThat(result.termSets()).isNotEmpty();
  }

  @Test
  public void buildComponentIndexQuery_putsComponentHashesInTermSetsNotString() {
    when(dimensionQueryBuilder.buildScopeFilterRestrictions(any(), any())).thenReturn(List.of());

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.componentHashes = Set.of("hash-b", "hash-a");

    ComponentsIndexQuery indexQuery = queryBuilder.buildComponentIndexQuery(request);

    assertThat(indexQuery.query()).doesNotContain("componentHash:(");
    assertThat(indexQuery.query()).doesNotContain("hash-a");
    assertThat(indexQuery.termSets()).hasSize(1);
    IndexTermSetRestriction restriction = (IndexTermSetRestriction) indexQuery.termSets().get(0);
    assertThat(restriction.field()).isEqualTo(FieldIdentifier.COMPONENT_HASH.label);
    assertThat(restriction.ids()).containsExactly("hash-a", "hash-b");
  }
}
