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
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class OrganizationListSelector
    extends OrganizationSelector
{
  protected static final String REPLACEMENT_PREFIX = "{orgIdList";

  @Override
  protected Map<String, List<String>> loadSelections(Connection connection, DbUtilParameters params) throws Exception {

    String query = buildOrgsByMostEvalsQuery(params, false);

    List<String> orgIds = new ArrayList<>();
    try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
      while (result.next()) {
        orgIds.add(result.getString(1));
      }
    }

    return Collections.singletonMap(REPLACEMENT_PREFIX, orgIds);
  }

  @Override
  public Replacer buildReplacer(Connection conn, DbUtilParameters params) throws Exception {
    return new ListReplacer(REPLACEMENT_PREFIX, loadSelections(conn, params).get(REPLACEMENT_PREFIX),
        params.getMaxOrganizations());
  }
}
