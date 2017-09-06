/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.SchemaInfoDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.db.H2DatabaseMigrator;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SchemaInfo;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PolicyDroolsCodeMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyDroolsCodeMigrator migrator;

  @Inject
  private InsightWork work;

  @Inject
  private SchemaInfoDAO schemaInfoDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Test
  public void testMigrate_FromBeforePersistedDroolsCode() throws Exception {
    // Create test data
    Application app = tempEntity.newApplicationWithParent("PolicyDroolsCodeMigratorTest-App",
        "PolicyDroolsCodeMigratorTestAppId");
    Policy policyApp = tempEntity.newPolicy(app.getId(), "policyApp");
    Policy policyOrg = tempEntity.newPolicy(app.getOrganizationId(), "policyOrg");

    // Fake schema state before migration
    SchemaInfo schemaInfo = schemaInfoDAO.get();
    schemaInfo.setDroolsCodeVersion(0);
    schemaInfoDAO.update(schemaInfo);

    // Change the db to have policy.drools_code nullable and all values null
    new H2DatabaseMigrator().runScript(OperationalDataStoreProvider.getDataSource(),
        "/PolicyDroolsCodeMigratorTest/set_policy_drools_code_to_null_success.sql");
    policyApp = policyDAO.getById(policyApp.getId());
    assertThat(policyApp.getDroolsCode(), is(nullValue()));
    policyOrg = policyDAO.getById(policyOrg.getId());
    assertThat(policyOrg.getDroolsCode(), is(nullValue()));

    // Run the migrator
    migrator.migrate();

    assertThat(schemaInfoDAO.get().getDroolsCodeVersion(), is(PolicyDroolsCodeMigrator.DROOLS_CODE_VERSION));

    // Assert the code was generated for all policies
    policyApp = policyDAO.getById(policyApp.getId());
    assertThat(policyApp.getDroolsCode(), is(notNullValue()));
    policyOrg = policyDAO.getById(policyOrg.getId());
    assertThat(policyOrg.getDroolsCode(), is(notNullValue()));

    // Assert that the policy.drools_code column is not nullable
    try {
      new H2DatabaseMigrator().runScript(OperationalDataStoreProvider.getDataSource(),
          "/PolicyDroolsCodeMigratorTest/set_policy_drools_code_to_null_fail.sql");
      fail("Expected exception");
    }
    catch (Exception e) {
      assertExceptionContains(e, "NULL not allowed for column \"drools_code\"");
    }
  }

  private void assertExceptionContains(Exception e, String message) throws Exception {
    Throwable cause = e;
    while (cause != null) {
      if (cause.getMessage() != null && cause.getMessage().contains(message)) {
        return;
      }
      cause = cause.getCause();
    }
    throw e;
  }

  @Test
  public void testMigrate_FromBeforePersistedDroolsCodeVersion() throws Exception {
    // Create test data
    Application app = tempEntity.newApplicationWithParent("PolicyDroolsCodeMigratorTest-App",
        "PolicyDroolsCodeMigratorTestAppId");
    Policy policyApp = tempEntity.newPolicy(app.getId(), "policyApp");
    Policy policyOrg = tempEntity.newPolicy(app.getOrganizationId(), "policyOrg");

    // Fake schema state before migration
    File markerFile = new File(work.getWorkDir(), PolicyDroolsCodeMigrator.MARKER_FILE_NAME);
    assertThat(markerFile.createNewFile(), is(true));
    SchemaInfo schemaInfo = schemaInfoDAO.get();
    schemaInfo.setDroolsCodeVersion(0);
    schemaInfoDAO.update(schemaInfo);

    // Run the migrator
    migrator.migrate();

    assertThat(schemaInfoDAO.get().getDroolsCodeVersion(), is(PolicyDroolsCodeMigrator.DROOLS_CODE_VERSION));
    assertThat(markerFile.exists(), is(false));

    // Assert the code was generated for all policies
    policyApp = policyDAO.getById(policyApp.getId());
    assertThat(policyApp.getDroolsCode(), is(notNullValue()));
    policyOrg = policyDAO.getById(policyOrg.getId());
    assertThat(policyOrg.getDroolsCode(), is(notNullValue()));
  }

  @Test
  public void testMigrate_GracefullyHandleInvalidPolicy() throws Exception {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Policy policy = new Policy(null, "Test Policy");
    policy.setOwnerId(org.getId());
    Constraint constraint = new Constraint(null, "Test Constrainst", LogicalOperator.OR);
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId()));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);

    new LicenseThreatGroupDAO().delete(ltg);
    ValidationResult validationResult = policy.validate(null, policy.getOwnerId());
    assertThat(validationResult.isValid(), is(false));

    SchemaInfo schemaInfo = schemaInfoDAO.get();
    schemaInfo.setDroolsCodeVersion(2);
    schemaInfoDAO.update(schemaInfo);

    migrator.migrate();

    assertThat(schemaInfoDAO.get().getDroolsCodeVersion(), is(PolicyDroolsCodeMigrator.DROOLS_CODE_VERSION));
  }

  @Test
  public void testMigrate_DeprecatedConditionForSecurityVulnerabilities() throws Exception {
    // Verifies that the deprecated condition for security vulnerabilities can be migrated.
    // The migrator should not fail when it encounters this policy condition type.
    String policyId = tempEntity.newPolicy("Test").getId();
    PolicyInternalDAO policyInternalDAO = new PolicyInternalDAO();
    PolicyInternal policyInternal = policyInternalDAO.getById(policyId);
    policyInternal.setContent(getPolicyContent("policy_deprecated_security_vulnerability_condition.json"));
    policyInternalDAO.update(policyInternal);

    // Fake schema state before migration
    SchemaInfo schemaInfo = schemaInfoDAO.get();
    schemaInfo.setDroolsCodeVersion(0);
    schemaInfoDAO.update(schemaInfo);

    migrator.migrate();
    Policy policy = policyDAO.getById(policyId);
    Condition deprecatedCondition = policy.getConstraints().get(0).getConditions().get(0);
    assertThat(deprecatedCondition.getConditionTypeId(), is("SecurityVulnerability"));
    assertThat(deprecatedCondition.getOperator(), is("present"));
    assertThat(deprecatedCondition.getValue(), is(nullValue()));
  }

  private String getPolicyContent(String filename) throws Exception {
    return IOUtil.toString(getClass().getResourceAsStream("/PolicyDroolsCodeMigratorTest/" + filename), "UTF-8");
  }
}
