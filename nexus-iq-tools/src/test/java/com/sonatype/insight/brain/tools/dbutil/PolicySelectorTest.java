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

public class PolicySelectorTest
    extends AbstractSelectorTest
{
  // VALUES('', '', '', '', '', 0, '', '', TS, TS, TS, false, false);
  private static final String[] POLICY_VIOLATION_COLS = new String[] { "policy_violation_id", "application_id",
      "stage_type_id", "policy_id", "policy_name", "threat_level", "threat_category", "constraint_facts_json",
      "open_time", "waive_time", "fix_time", "seen_by_primary_evaluation", "seen_by_monitoring_evaluation" };

  private void defaults() throws Exception {
    // open
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v1", "a1", "build", "p1", "pn1", 0, "test", "",
        "2018-01-01 00:00:00", null, null, Boolean.FALSE, Boolean.FALSE });
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v2", "a1", "build", "p1", "pn1", 0, "test", "",
        "2018-01-01 00:00:00", null, null, Boolean.FALSE, Boolean.FALSE });
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v3", "a2", "build", "p2", "pn2", 0, "test", "",
        "2018-01-01 00:00:00", null, null, Boolean.FALSE, Boolean.FALSE });
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v4", "a2", "build", "p2", "pn2", 0, "test", "",
        "2018-01-01 00:00:00", null, null, Boolean.FALSE, Boolean.FALSE });
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v5", "a3", "develop", "p3", "pn3", 0, "test", "",
        "2018-01-01 00:00:00", null, null, Boolean.FALSE, Boolean.FALSE });
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v6", "a4", "develop", "p4", "pn4", 0, "test", "",
        "2018-01-01 00:00:00", null, null, Boolean.FALSE, Boolean.FALSE });
    // waived
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v7", "a5", "develop", "p5", "pn5", 0, "test", "",
        "2018-01-01 00:00:00", "2018-10-01 00:00:00", null, Boolean.FALSE, Boolean.FALSE });
    // fixed
    insert("policy_violation", POLICY_VIOLATION_COLS, new Object[] { "v8", "a5", "develop", "p6", "pn6", 0, "test", "",
        "2018-01-01 00:00:00", null, "2018-02-20 00:00:00", Boolean.FALSE, Boolean.FALSE });
  }

  @Test
  public void testLoadSelections_Defaults() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters();
      Map<String, List<String>> selections = new PolicySelector().loadSelections(conn, params);

      assertThat(selections).hasSize(1);
      assertThat(selections.get(PolicySelector.REPLACEMENT_KEY)).containsExactlyInAnyOrder("p1", "p2", "p3", "p4", "p5",
          "p6");
    }
  }

  @Test
  public void testLoadSelections_ExcludeStage() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-xdevelop");
      Map<String, List<String>> selections = new PolicySelector().loadSelections(conn, params);

      assertThat(selections).hasSize(1);
      assertThat(selections.get(PolicySelector.REPLACEMENT_KEY)).containsExactlyInAnyOrder("p1", "p2");
    }
  }

  @Test
  public void testLoadSelections_MaxPolicies() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-max-pol", "2");
      Map<String, List<String>> selections = new PolicySelector().loadSelections(conn, params);

      assertThat(selections).hasSize(1);
      assertThat(selections.get(PolicySelector.REPLACEMENT_KEY)).containsExactlyInAnyOrder("p1", "p2");
    }
  }

  @Test
  public void testLoadSelections_ExcludeStates() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters excludeOpen = new DbUtilParameters("-xopen");
      DbUtilParameters excludeWaived = new DbUtilParameters("-xwaived");
      DbUtilParameters excludeFixed = new DbUtilParameters("-xfixed");

      DbUtilParameters excludeOpenWaived = new DbUtilParameters("-xopen", "-xwaived");
      DbUtilParameters excludeOpenFixed = new DbUtilParameters("-xopen", "-xfixed");

      DbUtilParameters excludeWaivedFixed = new DbUtilParameters("-xwaived", "-xfixed");

      String key = PolicySelector.REPLACEMENT_KEY;
      PolicySelector ps = new PolicySelector();

      assertThat(ps.loadSelections(conn, excludeOpen).get(key)).containsExactlyInAnyOrder("p5", "p6");
      assertThat(ps.loadSelections(conn, excludeWaived).get(key)).containsExactlyInAnyOrder("p1", "p2", "p3", "p4",
          "p6");
      assertThat(ps.loadSelections(conn, excludeFixed).get(key)).containsExactlyInAnyOrder("p1", "p2", "p3", "p4",
          "p5");
      assertThat(ps.loadSelections(conn, excludeOpenWaived).get(key)).containsExactlyInAnyOrder("p6");
      assertThat(ps.loadSelections(conn, excludeOpenFixed).get(key)).containsExactlyInAnyOrder("p5");
      assertThat(ps.loadSelections(conn, excludeWaivedFixed).get(key)).containsExactlyInAnyOrder("p1", "p2", "p3",
          "p4");
    }
  }
}
