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

public class ApplicationListSelector
    extends ApplicationEvalSelector
{
  protected static final String APPLICATION_LIST_REPLACEMENT_PREFIX = "{appIdList";

  @Override
  protected Map<String, List<String>> loadSelections(Connection connection, DbUtilParameters params) throws Exception {

    String queryApps = buildMostViolationsAppQuery(params, false);

    List<String> allApplicationIds = new ArrayList<>();

    try (Statement statement = connection.createStatement()) {
      try (ResultSet result = statement.executeQuery(queryApps)) {
        while (result.next()) {
          allApplicationIds.add(result.getString(1));
        }
      }
    }

    Map<String, List<String>> replacements = new HashMap<>();
    replacements.put(APPLICATION_LIST_REPLACEMENT_PREFIX, allApplicationIds);
    return replacements;
  }

  @Override
  public Replacer buildReplacer(Connection conn, DbUtilParameters params) throws Exception {
    return new ListReplacer(APPLICATION_LIST_REPLACEMENT_PREFIX,
        loadSelections(conn, params).get(APPLICATION_LIST_REPLACEMENT_PREFIX), params.getMaxApplications());
  }
}
