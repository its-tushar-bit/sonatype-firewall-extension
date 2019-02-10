/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StageComponentSelectorTest
    extends AbstractSelectorTest
{
  // VALUES('', '', '', '', '', 0, '', '', TS, TS, TS, false, false);
  private static final String[] POLICY_VIOLATION_COLS = new String[] { "policy_violation_id", "application_id",
      "stage_type_id", "policy_id", "policy_name", "threat_level", "hash", "threat_category", "constraint_facts_json",
      "open_time", "waive_time", "fix_time", "seen_by_primary_evaluation", "seen_by_monitoring_evaluation" };

  // VALUES('', '', '', '', '', '', '', '', '', '', false);
  private static final String[] APP_COMPONENT_COLS = new String[] { "application_component_id", "application_id",
      "stage_type_id", "time", "hash", "component_id_format", "component_id_coordinates_json", "match_state_id",
      "identification_source_id", "pathnames", "proprietary" };

  private void defaults() throws Exception {
    // just used to grab hash counts
    final String OPEN = "2018-01-01 00:00:00";
    final String WAIVED = "2018-02-01 00:00:00";
    final String FIXED = "2018-03-01 00:00:00";
    // 4 - h444
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v1", "a1", "build", "p1", "pn1", 0, "h444",
        "test", "", OPEN, null, null, Boolean.FALSE, Boolean.FALSE });
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v2", "a1", "build", "p1", "pn1", 0, "h444",
        "test", "", OPEN, null, null, Boolean.FALSE, Boolean.FALSE });
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v3", "a1", "build", "p1", "pn1", 0, "h444",
        "test", "", OPEN, null, null, Boolean.FALSE, Boolean.FALSE });
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v4", "a1", "build", "p1", "pn1", 0, "h444",
        "test", "", OPEN, null, null, Boolean.FALSE, Boolean.FALSE });
    // 2 - h222
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v5", "a1", "develop", "p1", "pn1", 0, "h222",
        "test", "", OPEN, WAIVED, null, Boolean.FALSE, Boolean.FALSE });
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v6", "a1", "build", "p1", "pn1", 0, "h222",
        "test", "", OPEN, WAIVED, null, Boolean.FALSE, Boolean.FALSE });
    // 1 - h110
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v7", "a1", "develop", "p1", "pn1", 0, "h110",
        "test", "", OPEN, null, FIXED, Boolean.FALSE, Boolean.FALSE });
    // 1 - h111
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v8", "a1", "build", "p1", "pn1", 0, "h111",
        "test", "", OPEN, null, FIXED, Boolean.FALSE, Boolean.FALSE });

    // app components
    insert("application_component", APP_COMPONENT_COLS,
        new Object[] { "ac1", "a1", "build", "2018-01-01 00:00:00", "h111", "", "", "", "", "", Boolean.FALSE });
    insert("application_component", APP_COMPONENT_COLS,
        new Object[] { "ac2", "a1", "develop", "2018-01-01 00:00:00", "h111", "", "", "", "", "", Boolean.FALSE });
    insert("application_component", APP_COMPONENT_COLS,
        new Object[] { "ac3", "a1", "build", "2018-01-01 00:00:00", "h222", "", "", "", "", "", Boolean.FALSE });
    insert("application_component", APP_COMPONENT_COLS,
        new Object[] { "ac4", "a1", "develop", "2018-01-01 00:00:00", "h222", "", "", "", "", "", Boolean.FALSE });
    insert("application_component", APP_COMPONENT_COLS,
        new Object[] { "ac5", "a1", "build", "2018-01-01 00:00:00", "h444", "", "", "", "", "", Boolean.FALSE });
    insert("application_component", APP_COMPONENT_COLS,
        new Object[] { "ac6", "a1", "develop", "2018-01-01 00:00:00", "h444", "", "", "", "", "", Boolean.FALSE });
  }

  static final String hKey = StageComponentSelector.COMPONENT_HASH_REPLACEMENT_KEY;

  static final String sKey = StageComponentSelector.STAGE_REPLACEMENT_KEY;

  @Test
  public void testLoadSelections_Defaults() throws Exception {
    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters();
      Map<String, List<String>> selections = new StageComponentSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(2);
      assertThat(selections.get(hKey)).containsExactly("h444", "h444", "h222", "h222", "h111", "h111");
      assertThat(selections.get(sKey)).containsExactly("build", "develop", "build", "develop", "build", "develop");
    }
  }

  @Test
  public void testLoadSelections_ExcludeStage() throws Exception {
    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-xbuild");
      Map<String, List<String>> selections = new StageComponentSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(2);
      assertThat(selections.get(hKey)).containsExactly("h222");
      assertThat(selections.get(sKey)).containsExactly("develop");
    }
  }

  @Test
  public void testLoadSelections_MaxComps() throws Exception {
    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-max-comp", "2");
      Map<String, List<String>> selections = new StageComponentSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(2);
      assertThat(selections.get(hKey)).containsExactly("h444", "h444");
      assertThat(selections.get(sKey)).containsExactly("build", "develop");
    }
  }

  @Test
  public void testLoadSelections_ViolationStates() throws Exception {
    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-xopen");
      Map<String, List<String>> selections = new StageComponentSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(2);
      assertThat(selections.get(hKey)).containsExactly("h222", "h222", "h111", "h111");
      assertThat(selections.get(sKey)).containsExactly("build", "develop", "build", "develop");
    }
  }
}
