/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PolicySelector
    extends AbstractSelector
{
  protected static final String REPLACEMENT_KEY = "{policyId}";

  private List<String> violationStateExclusions(DbUtilParameters params) {
    List<String> exclusions = new ArrayList<>();
    if (params.excludeOpen()) {
      exclusions.add("(waive_time IS NOT NULL OR fix_time IS NOT NULL)");
    }
    if (params.excludeWaived()) {
      exclusions.add("waive_time IS NULL");
    }
    if (params.excludeFixed()) {
      exclusions.add("fix_time IS NULL");
    }
    return exclusions;
  }

  @Override
  protected Map<String, List<String>> loadSelections(Connection connection, DbUtilParameters params) throws Exception {
    List<String> whereClauseList = new ArrayList<>();
    whereClauseList.add(getStageClause("stage_type_id", params));
    whereClauseList.addAll(violationStateExclusions(params));
    StringBuilder whereClause = new StringBuilder();
    for (String condition : whereClauseList) {
      if (condition != null) {
        if (whereClause.length() == 0) {
          whereClause.append(" WHERE ").append(condition);
        }
        else {
          whereClause.append(" AND ").append(condition);
        }
      }
    }

    String query = "" //
        + "SELECT policy_id, count(policy_violation_id) AS total_violations" //
        + " FROM policy_violation" //
        + whereClause //
        + " GROUP BY (policy_id)" //
        + " ORDER BY total_violations DESC" //
        + " LIMIT " + params.getMaxPolicies();

    List<String> polIds = new ArrayList<>();
    try (Statement statement = connection.createStatement()) {
      try (ResultSet result = statement.executeQuery(query)) {
        while (result.next()) {
          polIds.add(result.getString(1));
        }
      }
    }

    return Collections.singletonMap(REPLACEMENT_KEY, polIds);
  }
}
