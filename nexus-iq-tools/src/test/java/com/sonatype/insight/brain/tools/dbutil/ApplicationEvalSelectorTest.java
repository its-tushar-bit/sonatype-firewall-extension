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

public class ApplicationEvalSelectorTest
    extends AbstractSelectorTest
{
  // VALUES("", "", "", "", "", "", "")
  private static final String[] APPLICATION_COLS = new String[] { "application_id", "public_id", "public_id_lowercase",
      "name", "name_lowercase_no_whitespace", "organization_id", "contact_internal_name" };

  // VALUES("", "", "", "", Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, "");
  private static final String[] POLICY_EVAL_COLS = new String[] { "policy_evaluation_id", "application_id",
      "stage_type_id", "scan_id", "reevaluation", "for_monitoring", "for_obsolete_scan", "time", "initiator",
      "scan_trigger_type"};

  // VALUES('', '', '', '', '', 0, '', '', TS, TS, TS, false, false);
  private static final String[] POLICY_VIOLATION_COLS = new String[] { "policy_violation_id", "application_id",
      "stage_type_id", "policy_id", "policy_name", "threat_level", "threat_category", "constraint_facts_json",
      "open_time", "waive_time", "fix_time", "seen_by_primary_evaluation", "seen_by_monitoring_evaluation" };

  private void defaults() throws Exception {
    // applications
    insert("application", APPLICATION_COLS, new Object[] { "app1", "a1", "a1", "a1", "a1", "org1", "smitty" });
    insert("application", APPLICATION_COLS, new Object[] { "app2", "a2", "a2", "a2", "a2", "org2", "smitty" });
    insert("application", APPLICATION_COLS, new Object[] { "app3", "a3", "a3", "a3", "a3", "org3", "smitty" });
    // evaluations
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval1", "app1", "build", "scan1", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2001-01-01 00:00:00", "system", "CLI"});
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval2", "app2", "build", "scan2", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2002-02-02 00:00:00", "system", "CLI"});
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval3", "app2", "build", "scan3", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2003-03-03 00:00:00", "system", "CLI"});
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval4", "app3", "release", "scan4", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2004-04-04 00:00:00", "system", "CLI"});
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval5", "app1", "build", "scan5", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2005-05-05 00:00:00", "system", "CLI"});
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval6", "app1", "build", "scan6", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2006-06-06 00:00:00", "system", "CLI"});
    // violations
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
      Map<String, List<String>> selections = new ApplicationEvalSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(4);
      assertThat(selections.get(ApplicationEvalSelector.APPLICATION_REPLACEMENT_KEY)).containsExactlyInAnyOrder("app1",
          "app2", "app3");
    }
  }

  @Test
  public void testLoadSelections_ExcludeStage() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-xbuild");
      Map<String, List<String>> selections = new ApplicationEvalSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(4);
      assertThat(selections.get(ApplicationEvalSelector.APPLICATION_REPLACEMENT_KEY)).containsExactlyInAnyOrder("app3");
    }
  }

  @Test
  public void testLoadSelections_MaxEvals() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-max-eval", "2");
      Map<String, List<String>> selections = new ApplicationEvalSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(4);
      assertThat(selections.get(ApplicationEvalSelector.APPLICATION_REPLACEMENT_KEY)).containsExactlyInAnyOrder("app1",
          "app1", "app2", "app2", "app3");
    }
  }

  @Test
  public void testLoadSelections_MaxApps() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-max-app", "2");
      Map<String, List<String>> selections = new ApplicationEvalSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(4);
      assertThat(selections.get(ApplicationEvalSelector.APPLICATION_REPLACEMENT_KEY)).containsExactlyInAnyOrder("app1",
          "app2");
    }
  }

  @Test
  public void testLoadSelections_MaxAppsMaxEvals() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-max-app", "2", "-max-eval", "2");
      Map<String, List<String>> selections = new ApplicationEvalSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(4);
      assertThat(selections.get(ApplicationEvalSelector.APPLICATION_REPLACEMENT_KEY)).containsExactlyInAnyOrder("app1",
          "app1", "app2", "app2");
    }
  }
}
