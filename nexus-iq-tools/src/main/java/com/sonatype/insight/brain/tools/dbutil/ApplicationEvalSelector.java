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

public class ApplicationEvalSelector
    extends AbstractSelector
{
  protected static final String APPLICATION_PUBLIC_REPLACEMENT_KEY = "{applicationPublicId}";

  protected static final String APPLICATION_REPLACEMENT_KEY = "{applicationId}";

  protected static final String EVALUATION_REPLACEMENT_KEY = "{evaluationId}";

  protected static final String SCAN_REPLACEMENT_KEY = "{scanId}";

  protected String buildMostViolationsAppQuery(DbUtilParameters params, boolean limit) {
    String queryApps = "" //
        + "SELECT app.application_id," //
        + " (SELECT count(*)" //
        + " FROM insight_brain_ods.policy_violation pol WHERE pol.application_id = app.application_id" //
        + " AND " + getStageClause("pol.stage_type_id", params) //
        + " ) AS total_violations" //
        + " FROM insight_brain_ods.application app" //
        + " ORDER BY total_violations DESC, application_id ASC" //
        + (limit ? " LIMIT " + params.getMaxApplications() : "");
    return queryApps;
  }

  protected String buildEvalDetailsQuery(DbUtilParameters params, boolean limit) {
    String appEvalDetails = "" //
        + "SELECT app.public_id,"
        + " eval.application_id, eval.policy_evaluation_id, eval.scan_id, eval.stage_type_id, eval.time" //
        + " FROM insight_brain_ods.policy_evaluation eval, insight_brain_ods.application app" //
        + " WHERE eval.application_id = app.application_id" //
        + " AND " + getStageClause("eval.stage_type_id", params) //
        + " AND app.application_id = '" + APPLICATION_REPLACEMENT_KEY + "'" //
        + " ORDER BY eval.time DESC" //
        + (limit ? " LIMIT " + params.getMaxEvaluations() : "");
    return appEvalDetails;
  }

  @Override
  protected Map<String, List<String>> loadSelections(Connection connection, DbUtilParameters params) throws Exception {

    String queryApps = buildMostViolationsAppQuery(params, true);

    String appEvalDetails = buildEvalDetailsQuery(params, true);

    List<String> topApplicationIds = new ArrayList<>();

    List<String> applicationPublicIds = new ArrayList<>();
    List<String> applicationIds = new ArrayList<>();
    List<String> evaluationIds = new ArrayList<>();
    List<String> scanIds = new ArrayList<>();

    try (Statement statement = connection.createStatement()) {
      try (ResultSet result = statement.executeQuery(queryApps)) {
        while (result.next()) {
          topApplicationIds.add(result.getString(1));
        }
      }

      for (String appId : topApplicationIds) {
        try (ResultSet result = statement.executeQuery(appEvalDetails.replace(APPLICATION_REPLACEMENT_KEY, appId))) {
          while (result.next()) {
            applicationPublicIds.add(result.getString(1));
            applicationIds.add(result.getString(2));
            evaluationIds.add(result.getString(3));
            scanIds.add(result.getString(4));
          }
        }
      }
    }

    Map<String, List<String>> replacements = new HashMap<>();
    replacements.put(APPLICATION_PUBLIC_REPLACEMENT_KEY, applicationPublicIds);
    replacements.put(APPLICATION_REPLACEMENT_KEY, applicationIds);
    replacements.put(EVALUATION_REPLACEMENT_KEY, evaluationIds);
    replacements.put(SCAN_REPLACEMENT_KEY, scanIds);

    return replacements;
  }
}
