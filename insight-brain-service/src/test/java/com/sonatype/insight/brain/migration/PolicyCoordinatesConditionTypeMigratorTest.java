/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.migration.PolicyCoordinatesConditionTypeMigrator.MIGRATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyCoordinatesConditionTypeMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyCoordinatesConditionTypeMigrator migrator;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyInternalDAO policyInternalDAO;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Before
  public void before() {
    migrationTrackerDAO.deleteById(MIGRATION_ID);
  }

  @Test
  public void testMigrate_OnlyModifiesCoordinateConditions() throws Exception {
    assertThat(migrationTrackerDAO.getById(MIGRATION_ID)).isNull();

    // setup
    String originalCoordPolicyId = createObsoletePolicy("policy_gav.json");
    Policy originalVulnPolicy = newPolicy("vuln-policy", SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");

    // execute
    migrator.migrate();

    // assert
    Policy coordPolicy = policyDAO.getById(originalCoordPolicyId);
    Policy vulnPolicy = policyDAO.getById(originalVulnPolicy.getId());

    assertThat(getFirstConditionValue(coordPolicy)).isEqualTo(ComponentIdentifier.FORMAT_MAVEN + ":g:a:v:*:*");
    assertThat(getFirstConditionValue(vulnPolicy)).isEqualTo(getFirstConditionValue(originalVulnPolicy));

    assertThat(migrationTrackerDAO.getById(MIGRATION_ID)).isNotNull();
  }

  @Test
  public void testMigrate_WithNoPoliciesStillTracksMigration() {
    assertThat(migrationTrackerDAO.getById(MIGRATION_ID)).isNull();

    // execute
    migrator.migrate();

    // assert
    assertThat(policyDAO.getAll()).isEmpty();
    assertThat(migrationTrackerDAO.getById(MIGRATION_ID)).isNotNull();
  }

  /**
   * Using a single transaction maintains data integrity in case of an exception so that we don't update some
   * policies more than once.
   */
  @Test
  public void testMigrate_UsesSingleTransaction() {
    // setup
    TransactionContext txMock = mock(TransactionContext.class);
    PolicyDAO policyDAOMock = mock(PolicyDAO.class);
    when(policyDAOMock.createTransactionContext()).thenReturn(txMock);
    Policy policy1 = newPolicyObject("coord-policy1", CoordinatesConditionType.ID, "match", "maven:foo*");
    Policy policy2 = newPolicyObject("coord-policy2", CoordinatesConditionType.ID, "match", "maven:bar*");
    when(policyDAOMock.getAll(txMock)).thenReturn(Arrays.asList(policy1, policy2));
    migrator = new PolicyCoordinatesConditionTypeMigrator(policyDAOMock, migrationTrackerDAO);

    // execute
    migrator.migrate();

    // assert
    verify(policyDAOMock).update(txMock, policy1);
    verify(policyDAOMock).update(txMock, policy2);
    verify(txMock, times(1)).commit();
  }

  @Test
  public void testMigrate_AlreadyRunDoesNotMigratePolicies() {
    migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));

    Policy originalCoordPolicy = newPolicy("coord-policy", CoordinatesConditionType.ID, "match", "maven:foo*");

    // execute
    migrator.migrate();

    // assert
    Policy coordPolicy = policyDAO.getById(originalCoordPolicy.getId());
    assertThat(getFirstConditionValue(coordPolicy)).isEqualTo(getFirstConditionValue(originalCoordPolicy));
  }

  private Policy newPolicy(String name, String conditionTypeId, String operator, String conditionValue) {
    Policy policy = newPolicyObject(name, conditionTypeId, operator, conditionValue);
    return tempEntity.newPolicy(policy);
  }

  private Policy newPolicyObject(String name, String conditionTypeId, String operator, String conditionValue) {
    Policy policy = new Policy();
    policy.setName(name);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(conditionTypeId, operator, conditionValue));
    policy.addConstraint(constraint);
    return policy;
  }

  private String getFirstConditionValue(Policy policy) {
    return policy.getConstraints().get(0).getConditions().get(0).getValue();
  }

  private String getPolicyContent(String filename) throws IOException {
    return IOUtils.toString(getClass().getResourceAsStream("/PolicyCoordinatesConditionTypeMigratorTest/" + filename),
        StandardCharsets.UTF_8);
  }

  private String createObsoletePolicy(String policyJsonResourceName) throws IOException {
    String policyId = tempEntity.newPolicy().getId();
    PolicyInternal policyInternal = policyInternalDAO.getById(policyId);
    policyInternal.setContent(getPolicyContent(policyJsonResourceName));
    policyInternalDAO.update(policyInternal);

    return policyId;
  }

  @Test
  public void testMigrate_GAV() throws Exception {
    // The policy condition to be migrated has value="g:a:v".
    testMigrate("policy_gav.json", "maven:g:a:v:*:*");
  }

  @Test
  public void testMigrate_Wildcard() throws Exception {
    // The policy condition to be migrated has value="g:*".
    testMigrate("policy_wildcard.json", "maven:g:*:*:*:*");
  }

  private void testMigrate(String policyJsonResourceName, String expectedConditionValue) throws Exception {
    String policyId = createObsoletePolicy(policyJsonResourceName);

    migrator.migrate();

    Policy migratedPolicy = policyDAO.getById(policyId);
    assertThat(getFirstConditionValue(migratedPolicy)).isEqualTo(expectedConditionValue);

    assertThat(migrationTrackerDAO.getById(MIGRATION_ID)).isNotNull();
  }
}
