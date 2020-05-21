
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StageComponentSelector
    extends AbstractSelector
{
  protected static final String STAGE_REPLACEMENT_KEY = "{stageId}";

  protected static final String COMPONENT_HASH_REPLACEMENT_KEY = "{componentHash}";

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
    String stageClause = getStageClause("stage_type_id", params);
    whereClauseList.add(stageClause);
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

    String hashListQuery = "" //
        + "SELECT hash, count(hash) AS vcount FROM policy_violation" //
        + whereClause //
        + " GROUP BY (hash)" //
        + " ORDER BY vcount DESC" //
        + " LIMIT " + params.getMaxComponents();

    String distinctQuery = "" //
        + "SELECT DISTINCT stage_type_id, hash FROM application_component" //
        + " WHERE hash = {hashId}" //
        + ( stageClause != null ? " AND " + stageClause : " " ) //
        + " ORDER BY stage_type_id" //
        + " LIMIT " + params.getMaxComponents();

    List<String> topHashes = new ArrayList<>();

    List<String> stageIds = new ArrayList<>();
    List<String> componentHashes = new ArrayList<>();
    try (Statement statement = connection.createStatement()) {
      try (ResultSet result = statement.executeQuery(hashListQuery)) {
        while (result.next()) {
          topHashes.add(quote(result.getString(1)));
        }
      }
      for (String hash : topHashes) {
        String query = distinctQuery.replace("{hashId}", hash);

        try (ResultSet result = statement.executeQuery(query)) {
          while (result.next() && stageIds.size() < params.getMaxComponents()) {
            stageIds.add(result.getString(1));
            componentHashes.add(result.getString(2));
          }
        }
      }
    }

    Map<String, List<String>> replacements = new HashMap<>();
    replacements.put(STAGE_REPLACEMENT_KEY, stageIds);
    replacements.put(COMPONENT_HASH_REPLACEMENT_KEY, componentHashes);

    return replacements;
  }
}
