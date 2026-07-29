/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.search.index.ItemType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentsListIndexQueryBuilderTest
{
  @Mock
  private DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  @Mock
  private ComponentsListViolationScopeResolver violationScopeResolver;

  private ComponentsListIndexQueryBuilder queryBuilder;

  @Before
  public void setUp() {
    queryBuilder = new ComponentsListIndexQueryBuilder(dimensionQueryBuilder, violationScopeResolver);
  }

  @Test
  public void buildComponentQuery_includesComponentItemTypes() {
    when(dimensionQueryBuilder.buildOrganizationFilterClause(any())).thenReturn(null);
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(any())).thenReturn(null);

    String query = queryBuilder.buildComponentQuery(new ComponentsListRequestDTO());

    assertThat(query).contains(ItemType.NON_VULNERABLE_COMPONENT.name());
    assertThat(query).contains(ItemType.SECURITY_VULNERABILITY.name());
  }

  @Test
  public void buildComponentQuery_includesSearchAcrossComponentFields() {
    when(dimensionQueryBuilder.buildOrganizationFilterClause(any())).thenReturn(null);
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(any())).thenReturn(null);

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.search = "log4j";

    String query = queryBuilder.buildComponentQuery(request);

    assertThat(query).contains("componentName:");
    assertThat(query).contains("componentHash:");
    assertThat(query).contains("log4j");
  }

  @Test
  public void buildComponentQuery_appliesOrganizationFilter() {
    when(dimensionQueryBuilder.buildOrganizationFilterClause(Set.of("org-1")))
        .thenReturn("organizationId:(org-1)");
    when(dimensionQueryBuilder.buildEscapedApplicationFilterClause(any())).thenReturn(null);

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.organizationIds = Set.of("org-1");

    String query = queryBuilder.buildComponentQuery(request);

    assertThat(query).contains("organizationId:(org-1)");
  }
}
