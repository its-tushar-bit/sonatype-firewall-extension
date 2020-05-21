/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.stages.StageTypes;

public abstract class AbstractSelector
    implements ReplacementSource
{
  protected abstract Map<String, List<String>> loadSelections(Connection conn, DbUtilParameters params)
      throws Exception;

  public static String quote(String val) {
    return "'" + val + "'";
  }

  @Override
  public Replacer buildReplacer(Connection conn, DbUtilParameters params) throws Exception {
    return new Replacer(loadSelections(conn, params));
  }

  protected String getStageClause(String stageField, DbUtilParameters params) {
    List<String> excludedStages = new ArrayList<>();

    if (params.excludeBuild()) {
      excludedStages.add(quote(StageTypes.BUILD.getId()));
    }
    if (params.excludeDevelop()) {
      excludedStages.add(quote(StageTypes.DEVELOP.getId()));
    }
    if (params.excludeOperate()) {
      excludedStages.add(quote(StageTypes.OPERATE.getId()));
    }
    if (params.excludeProxy()) {
      excludedStages.add(quote(StageTypes.PROXY.getId()));
    }
    if (params.excludeRelease()) {
      excludedStages.add(quote(StageTypes.RELEASE.getId()));
    }
    if (params.excludeStage()) {
      excludedStages.add(quote(StageTypes.STAGE_RELEASE.getId()));
    }

    if (!excludedStages.isEmpty()) {
      String clauseTemp = "{field} NOT IN ({clauseList})";
      return clauseTemp.replace("{field}", stageField).replace("{clauseList}", String.join(",", excludedStages));
    }
    else {
      return null;
    }
  }
}
