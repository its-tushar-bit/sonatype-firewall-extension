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

public class OrganizationSelectorTest
    extends AbstractSelectorTest
{
  // VALUES("", "", "", "", "", "", "")
  private static final String[] APPLICATION_COLS = new String[] { "application_id", "public_id", "public_id_lowercase",
      "name", "name_lowercase_no_whitespace", "organization_id", "contact_internal_name" };

  // VALUES("", "", "", "", Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, "");
  private static final String[] POLICY_EVAL_COLS = new String[] { "policy_evaluation_id", "application_id",
      "stage_type_id", "scan_id", "reevaluation", "for_monitoring", "for_obsolete_scan", "time", "initiator",
      "scan_trigger_type"};

  private void defaults() throws Exception {
    insert("application", APPLICATION_COLS, new Object[] { "app1", "a1", "a1", "a1", "a1", "org1", "smitty" });
    insert("application", APPLICATION_COLS, new Object[] { "app2", "a2", "a2", "a2", "a2", "org2", "smitty" });
    insert("application", APPLICATION_COLS, new Object[] { "app3", "a3", "a3", "a3", "a3", "org3", "smitty" });
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval1", "app1", "build", "scan1", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2018-01-01 00:00:00", "system", "CLI"});
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval2", "app2", "build", "scan2", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2018-01-01 00:00:00", "system", "CLI"});
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval3", "app2", "build", "scan3", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2018-01-01 00:00:00", "system", "CLI"});
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval4", "app3", "release", "scan4", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2018-01-01 00:00:00", "system", "CLI"});
    insert("policy_evaluation", POLICY_EVAL_COLS, new Object[] { "peval5", "app1", "build", "scan5", Boolean.FALSE,
        Boolean.FALSE, Boolean.FALSE, "2018-01-01 00:00:00", "system", "CLI"});
  }

  @Test
  public void testLoadSelections_Defaults() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters();
      Map<String, List<String>> selections = new OrganizationSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(1);
      assertThat(selections.get(OrganizationSelector.REPLACEMENT_KEY)).containsExactlyInAnyOrder("org1", "org2",
          "org3");
    }
  }

  @Test
  public void testLoadSelections_ExcludeStage() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-xbuild");
      Map<String, List<String>> selections = new OrganizationSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(1);
      assertThat(selections.get(OrganizationSelector.REPLACEMENT_KEY)).containsExactlyInAnyOrder("org3");
    }
  }

  @Test
  public void testLoadSelections_MaxOrgs() throws Exception {

    defaults();

    try (Connection conn = getConnection()) {
      DbUtilParameters params = new DbUtilParameters("-max-org", "2");
      Map<String, List<String>> selections = new OrganizationSelector().loadSelections(conn, params);

      assertThat(selections).hasSize(1);
      assertThat(selections.get(OrganizationSelector.REPLACEMENT_KEY)).containsExactlyInAnyOrder("org1", "org2");
    }
  }
}
