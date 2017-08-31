/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Lists;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyCoordinatesConditionTypeMigratorTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private InsightWork work;

  private PolicyCoordinatesConditionTypeMigrator migrator;

  private PolicyDAO policyDAO;

  private PolicyInternalDAO policyInternalDAO;

  @Before
  public void setUp() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    work = new InsightWork(insightConfig);
    work.getDataDir().mkdirs();
    policyDAO = new PolicyDAO();
    policyInternalDAO = new PolicyInternalDAO();
    migrator = new PolicyCoordinatesConditionTypeMigrator(work, policyDAO, policyInternalDAO);
  }

  @Test
  public void testMigrate_OnlyModifiesCoordinateConditions() throws Exception {
    // setup
    String originalCoordPolicyId = createObsoletePolicy("policy_gav.json");
    Policy originalVulnPolicy = newPolicy("vuln-policy", SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");

    // execute
    migrator.migrate();

    // assert
    Policy coordPolicy = policyDAO.getById(originalCoordPolicyId);
    Policy vulnPolicy = policyDAO.getById(originalVulnPolicy.getId());

    assertThat(getFirstConditionValue(coordPolicy), is(ComponentIdentifier.FORMAT_MAVEN + ":g:a:v:*:*"));
    assertThat(getFirstConditionValue(vulnPolicy), is(getFirstConditionValue(originalVulnPolicy)));

    File markerFile = new File(work.getWorkDir(), PolicyCoordinatesConditionTypeMigrator.MARKER_FILE_NAME);
    assertThat(markerFile.exists(), is(true));
  }

  @Test
  public void testMigrate_WithNoPoliciesStillCreatesMarkerFile() throws Exception {
    // execute
    migrator.migrate();

    // assert
    assertThat(policyDAO.getAll(), is(empty()));

    File markerFile = new File(work.getWorkDir(), PolicyCoordinatesConditionTypeMigrator.MARKER_FILE_NAME);
    assertThat(markerFile.exists(), is(true));
  }

  /**
   * Using a single transaction maintains data integrity in case of an exception so that we don't update some
   * policies more than once.
   */
  @Test
  public void testMigrate_UsesSingleTransaction() throws Exception {
    // setup
    TransactionContext txMock = mock(TransactionContext.class);
    PolicyInternalDAO policyInternalDAOMock = mock(PolicyInternalDAO.class);
    when(policyInternalDAOMock.createTransactionContext()).thenReturn(txMock);
    PolicyDAO policyDAOMock = mock(PolicyDAO.class);
    Policy policy1 = newPolicyObject("coord-policy1", CoordinatesConditionType.ID, "match", "maven:foo*");
    Policy policy2 = newPolicyObject("coord-policy2", CoordinatesConditionType.ID, "match", "maven:bar*");
    List<Policy> policies = Lists.newArrayList(policy1, policy2);
    when(policyDAOMock.getAll(txMock)).thenReturn(policies);
    migrator = new PolicyCoordinatesConditionTypeMigrator(work, policyDAOMock, policyInternalDAOMock);

    // execute
    migrator.migrate();

    // assert
    verify(policyDAOMock).update(txMock, policy1);
    verify(policyDAOMock).update(txMock, policy2);
    verify(txMock, times(1)).commit();
  }

  @Test
  public void testMigrate_AlreadyRunDoesNotMigratePolicies() throws Exception {
    // setup
    File markerFile = new File(work.getWorkDir(), PolicyCoordinatesConditionTypeMigrator.MARKER_FILE_NAME);
    markerFile.createNewFile();
    Policy originalCoordPolicy = newPolicy("coord-policy", CoordinatesConditionType.ID, "match", "maven:foo*");

    // execute
    migrator.migrate();

    // assert
    assertThat(markerFile.exists(), is(true));
    Policy coordPolicy = policyDAO.getById(originalCoordPolicy.getId());
    assertThat(getFirstConditionValue(coordPolicy), is(getFirstConditionValue(originalCoordPolicy)));
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
    return IOUtil.toString(getClass().getResourceAsStream("/PolicyCoordinatesConditionTypeMigratorTest/" + filename),
        "UTF-8");
  }

  private String createObsoletePolicy(String policyJsonResourceName) throws IOException {
    String policyId = tempEntity.newPolicy("Test").getId();
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
    assertThat(getFirstConditionValue(migratedPolicy), is(expectedConditionValue));

    File markerFile = new File(work.getWorkDir(), PolicyCoordinatesConditionTypeMigrator.MARKER_FILE_NAME);
    assertThat(markerFile.exists(), is(true));
  }
}
