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

public class OrganizationSelector
    extends AbstractSelector
{
  protected static final String REPLACEMENT_KEY = "{organizationId}";

  protected String buildOrgsByMostEvalsQuery(DbUtilParameters params, boolean limit) {
    String stageClause = getStageClause("peval.stage_type_id", params);
    String query = "" //
        + "SELECT app.organization_id, count(peval.policy_evaluation_id) AS stage_evals," //
        + " (SELECT count(*) FROM application appx WHERE appx.organization_id = app.organization_id) AS app_count" //
        + " FROM policy_evaluation peval, application app" //
        + " WHERE peval.application_id = app.application_id" //
        + ( stageClause != null ? " AND " + stageClause : " " ) //
        + " GROUP BY (app.organization_id)" //
        + " ORDER BY stage_evals DESC" //
        + (limit ? " LIMIT " + params.getMaxOrganizations() : "");
    return query;
  }

  @Override
  protected Map<String, List<String>> loadSelections(Connection connection, DbUtilParameters params) throws Exception {

    String query = buildOrgsByMostEvalsQuery(params, true);

    List<String> orgIds = new ArrayList<>();
    try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
      while (result.next()) {
        orgIds.add(result.getString(1));
      }
    }

    return Collections.singletonMap(REPLACEMENT_KEY, orgIds);
  }
}
