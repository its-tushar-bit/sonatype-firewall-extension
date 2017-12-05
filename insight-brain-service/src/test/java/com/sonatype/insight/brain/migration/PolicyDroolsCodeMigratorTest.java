/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.SchemaInfoDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
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

import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyDroolsCodeMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyDroolsCodeMigrator migrator;

  @Inject
  private SchemaInfoDAO schemaInfoDAO;

  @Inject
  private PolicyDAO policyDAO;

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
    schemaInfo.setDroolsCodeVersion(2);
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
