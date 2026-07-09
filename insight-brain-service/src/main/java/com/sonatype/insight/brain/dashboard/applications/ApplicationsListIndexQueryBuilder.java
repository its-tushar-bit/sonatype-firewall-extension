/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds RBAC-scoped Lucene queries for the Martha Applications list.
 */
@Named
final class ApplicationsListIndexQueryBuilder
{
  private final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder;

  @Inject
  ApplicationsListIndexQueryBuilder(final DashboardIndexDimensionQueryBuilder dimensionQueryBuilder) {
    this.dimensionQueryBuilder = dimensionQueryBuilder;
  }

  String buildApplicationQuery(final ApplicationsListRequestDTO request) {
    List<String> clauses = new ArrayList<>();
    clauses.add("itemType:" + ItemType.APPLICATION.name());

    String searchClause = buildSearchClause(request == null ? null : request.search);
    if (searchClause != null) {
      clauses.add(searchClause);
    }

    Set<String> organizationIds = request == null ? null : request.organizationIds;
    if (organizationIds != null && !organizationIds.isEmpty()) {
      DashboardIndexDimensionQueryBuilder.rejectBlankFilterIds(organizationIds, "organizationIds");
    }
    String organizationClause = dimensionQueryBuilder.buildOrganizationFilterClause(organizationIds);
    String applicationClause =
        dimensionQueryBuilder.buildEscapedApplicationFilterClause(request == null ? null : request.applicationIds);
    if (organizationClause != null || applicationClause != null) {
      List<String> dimensionClauses = new ArrayList<>();
      if (organizationClause != null) {
        dimensionClauses.add(organizationClause);
      }
      if (applicationClause != null) {
        dimensionClauses.add(applicationClause);
      }
      clauses.add("(" + String.join(" OR ", dimensionClauses) + ")");
    }

    return String.join(" AND ", clauses);
  }

  private static String buildSearchClause(final String search) {
    if (StringUtils.isBlank(search)) {
      return null;
    }
    // Leading-wildcard clauses mirror global search; index-side optimizations can follow scale testing.
    String[] tokens = search.trim().split("\\s+");
    List<String> tokenClauses = new ArrayList<>(tokens.length);
    for (String token : tokens) {
      if (StringUtils.isBlank(token)) {
        continue;
      }
      String safe = DashboardIndexDimensionQueryBuilder.escapeLuceneTerm(token);
      tokenClauses.add("(" + String.join(
          " OR ",
          "applicationName:*" + safe + "*",
          "applicationPublicId:*" + safe + "*",
          "organizationName:*" + safe + "*") + ")");
    }
    if (tokenClauses.isEmpty()) {
      return null;
    }
    if (tokenClauses.size() == 1) {
      return tokenClauses.get(0);
    }
    return "(" + String.join(" AND ", tokenClauses) + ")";
  }
}
