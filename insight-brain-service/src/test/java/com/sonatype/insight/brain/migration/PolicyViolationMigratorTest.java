/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PolicyViolationMigratorTest
    extends AbstractDatabaseTest
{
  private static class PolicyViolation
  {
    String applicationId;

    String stageTypeId;

    String policyId;

    String policyName;

    int threatLevel;

    String threatCategory;

    String hash;

    ComponentIdentifier componentIdentifer;

    String filename;

    String actionTypeId;

    String constraintFacts;

    String policyWaiverId;

    String policyWaiverComment;

    Timestamp openTime;

    Timestamp waiveTime;

    Timestamp fixTime;

    boolean seenByPrimaryEvaluation;

    boolean seenByMonitoringEvaluation;

    @SuppressWarnings("unchecked")
    PolicyViolation(ResultSet resultSet) throws Exception {
      applicationId = resultSet.getString("application_id");
      stageTypeId = resultSet.getString("stage_type_id");
      policyId = resultSet.getString("policy_id");
      policyName = resultSet.getString("policy_name");
      threatLevel = resultSet.getInt("threat_level");
      threatCategory = resultSet.getString("threat_category");
      hash = resultSet.getString("hash");
      String componentIdFormat = resultSet.getString("component_id_format");
      if (componentIdFormat != null) {
        componentIdentifer = new ComponentIdentifier(componentIdFormat,
            JsonUtils.parse(resultSet.getString("component_id_coordinates_json"), Map.class));
      }
      filename = resultSet.getString("filename");
      actionTypeId = resultSet.getString("action_type_id");
      constraintFacts = resultSet.getString("constraint_facts_json");
      policyWaiverId = resultSet.getString("policy_waiver_id");
      policyWaiverComment = resultSet.getString("policy_waiver_comment");
      openTime = resultSet.getTimestamp("open_time");
      waiveTime = resultSet.getTimestamp("waive_time");
      fixTime = resultSet.getTimestamp("fix_time");
      seenByPrimaryEvaluation = resultSet.getBoolean("seen_by_primary_evaluation");
      seenByMonitoringEvaluation = resultSet.getBoolean("seen_by_monitoring_evaluation");
    }
  }

  private void runScript(String scriptName) throws Exception {
    String scriptResource;
    if (scriptName.startsWith("schema_incremental_")) {
      scriptResource = "db/insight_brain_ods/" + scriptName;
    }
    else {
      scriptResource = getClass().getSimpleName() + '/' + scriptName;
    }
    new LegacyDataStoreMigrator(databaseRule.getOperationalDataStore())
        .runScript("", scriptResource + ".sql");
  }

  private void populateH2Database(String scriptName) throws Exception {
    runScript("schema");
    runScript("schema_incremental_0115");
    runScript(scriptName);
  }

  private List<PolicyViolation> migrate(String testScriptName) throws Exception {
    populateH2Database(testScriptName);
    DataSource dataSource = databaseRule.getOperationalDataStore().getDataSource();
    new PolicyViolationMigrator().migrate(dataSource, databaseRule.getOperationalDataStore().getDatabaseSchema());
    runScript("schema_incremental_0116");
    return loadViolations(dataSource);
  }

  private List<PolicyViolation> loadViolations(DataSource dataSource) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement select = connection.prepareStatement("SELECT * FROM insight_brain_ods.policy_violation"
            + " ORDER BY application_id, stage_type_id, open_time, policy_violation_id"))
    {
      List<PolicyViolation> violations = new ArrayList<>();
      try (ResultSet resultSet = select.executeQuery()) {
        while (resultSet.next()) {
          violations.add(new PolicyViolation(resultSet));
        }
        return violations;
      }
    }
  }

  private static Timestamp parseTimestamp(String timestamp) {
    if (timestamp == null) {
      return null;
    }
    try {
      return new Timestamp(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(timestamp).getTime());
    }
    catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  private void assertViolation(
      PolicyViolation violation,
      String applicationId,
      String stageTypeId,
      String policyId,
      String policyName,
      int threatLevel,
      String threatCategory,
      String hash,
      ComponentIdentifier componentIdentifer,
      String filename,
      String actionTypeId,
      String constraintFacts,
      String policyWaiverId,
      String policyWaiverComment,
      String openTime,
      String waiveTime,
      String fixTime,
      boolean seenByPrimaryEvaluation,
      boolean seenByMonitoringEvaluation)
  {
    assertThat(violation.applicationId).isEqualTo(applicationId);
    assertThat(violation.stageTypeId).isEqualTo(stageTypeId);
    assertThat(violation.policyId).isEqualTo(policyId);
    assertThat(violation.policyName).isEqualTo(policyName);
    assertThat(violation.threatLevel).isEqualTo(threatLevel);
    assertThat(violation.threatCategory).isEqualTo(threatCategory);
    assertThat(violation.hash).isEqualTo(hash);
    assertThat(violation.componentIdentifer).isEqualTo(componentIdentifer);
    assertThat(violation.filename).isEqualTo(filename);
    assertThat(violation.actionTypeId).isEqualTo(actionTypeId);
    assertThat(violation.constraintFacts).isEqualTo(constraintFacts);
    assertThat(violation.policyWaiverId).isEqualTo(policyWaiverId);
    assertThat(violation.policyWaiverComment).isEqualTo(policyWaiverComment);
    assertThat(violation.openTime).isEqualTo(parseTimestamp(openTime));
    assertThat(violation.waiveTime).isEqualTo(parseTimestamp(waiveTime));
    assertThat(violation.fixTime).isEqualTo(parseTimestamp(fixTime));
    assertThat(violation.seenByPrimaryEvaluation).isEqualTo(seenByPrimaryEvaluation);
    assertThat(violation.seenByMonitoringEvaluation).isEqualTo(seenByMonitoringEvaluation);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_Basics() throws Exception {
    List<PolicyViolation> violations = migrate("basics");
    assertThat(violations).hasSize(3);
    assertViolation(violations.get(0), "app-0", "build", "policy-0", "Policy 0", 5, "security", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "test-1.0.tgz", "fail", "constraints-0", null, null,
        "2018-02-01 01:23:45", null, null, true, false);
    assertViolation(violations.get(1), "app-0", "release", "policy-1", "Policy 1", 10, "license", "hash-1", null,
        "some.zip", "warn", "constraints-1", "waiver-0", "waiver-0-comment", "2018-02-02 01:23:45",
        "2018-02-02 01:23:45", null, false, false);
    assertViolation(violations.get(2), "app-0", "release", "policy-2", "Policy 2", 0, "other", "hash-2", null, null,
        null, "constraints-2", null, null, "2018-02-02 01:23:45", null, null, false, true);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_OpenToWaived() throws Exception {
    List<PolicyViolation> violations = migrate("open-to-waived");
    assertThat(violations).hasSize(1);
    assertViolation(violations.get(0), "app-0", "build", "policy-0", "Policy 0", 5, "security", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "test-1.0.tgz", "fail", "constraints-0", "waiver-0",
        "waiver-0-comment", "2018-02-01 01:23:45", "2018-02-02 01:23:45", null, false, false);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_OpenToWaivedToFixed() throws Exception {
    List<PolicyViolation> violations = migrate("open-to-waived-to-fixed");
    assertThat(violations).hasSize(1);
    assertViolation(violations.get(0), "app-0", "build", "policy-0", "Policy 0", 5, "security", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "test-1.0.tgz", "fail", "constraints-0", "waiver-0",
        "waiver-0-comment", "2018-02-01 01:23:45", "2018-02-02 01:23:45", "2018-02-03 01:23:45", false, false);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_OpenToWaivedToOpen() throws Exception {
    List<PolicyViolation> violations = migrate("open-to-waived-to-open");
    assertThat(violations).hasSize(2);
    assertViolation(violations.get(0), "app-0", "build", "policy-0", "Policy 0", 5, "security", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "test-1.0.tgz", "fail", "constraints-0", "waiver-0",
        "waiver-0-comment", "2018-02-01 01:23:45", "2018-02-02 01:23:45", "2018-02-03 01:23:45", false, false);
    assertViolation(violations.get(1), "app-0", "build", "policy-0", "Policy 0", 5, "security", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "test-1.0.tgz", "fail", "constraints-0", null, null,
        "2018-02-03 01:23:45", null, null, true, false);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_OpenToFixed() throws Exception {
    List<PolicyViolation> violations = migrate("open-to-fixed");
    assertThat(violations).hasSize(1);
    assertViolation(violations.get(0), "app-0", "build", "policy-0", "Policy 0", 5, "security", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "test-1.0.tgz", "fail", "constraints-0", null, null,
        "2018-02-01 01:23:45", null, "2018-02-02 01:23:45", true, false);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_OpenToFixedToOpen() throws Exception {
    List<PolicyViolation> violations = migrate("open-to-fixed-to-open");
    assertThat(violations).hasSize(2);
    assertViolation(violations.get(0), "app-0", "build", "policy-0", "Policy 0", 5, "security", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "test-1.0.tgz", "fail", "constraints-0", null, null,
        "2018-02-01 01:23:45", null, "2018-02-02 01:23:45", true, false);
    assertViolation(violations.get(1), "app-0", "build", "policy-0", "Policy 0", 5, "security", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "test-1.0.tgz", "fail", "constraints-0", null, null,
        "2018-02-03 01:23:45", null, null, true, false);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_LatestViolationState() throws Exception {
    List<PolicyViolation> violations = migrate("latest-violation-state");
    assertThat(violations).hasSize(1);
    assertViolation(violations.get(0), "app-0", "build", "policy-0", "Policy 0", 5, "other", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "new-1.0.tgz", "warn", "constraints-1", null, null,
        "2018-02-01 01:23:45", null, null, true, false);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_InitialWaiverInfo() throws Exception {
    List<PolicyViolation> violations = migrate("initial-waiver-info");
    assertThat(violations).hasSize(1);
    assertViolation(violations.get(0), "app-0", "build", "policy-0", "Policy 0", 5, "security", "hash-0",
        ComponentIdentifier.createNpmCoordinates("test", "1.0"), "test-1.0.tgz", "fail", "constraints-0", "waiver-0",
        "waiver-0-comment", "2018-02-01 01:23:45", "2018-02-01 01:23:45", null, false, false);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_ObsoleteReevaluation() throws Exception {
    List<PolicyViolation> violations = migrate("obsolete-reevaluation");
    assertThat(violations).isEmpty();
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_BrokenViolation() {
    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> migrate("broken-violation"))
        .withMessageContaining("eval-0-vio-0");
  }
}
