/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantPolicyStatusOverrideTelemetryCollectorTest
    extends AbstractMultiTenantDatabaseTest
{
  private PolicyStatusOverrideTelemetryCollector telemetryCollector;

  private PolicyWaiverDAO policyWaiverDAO;

  private OrganizationDAO organizationDAO;

  private ApplicationDAO applicationDAO;

  private PolicyDAO policyDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO =
        daoFactory.createSecurityVulnerabilityOverrideDAO();
    policyDAO = daoFactory.createPolicyDAO();
    policyWaiverDAO = daoFactory.createPolicyWaiverDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
    applicationDAO = daoFactory.createApplicationDAO();
    telemetryCollector = new PolicyStatusOverrideTelemetryCollector(securityVulnerabilityOverrideDAO, policyWaiverDAO);
  }

  @Test
  public void testShouldNotLeakDataBetweenTenants_whenMultiTenantMode() {
    testAsNewTenant(t1 -> {
      Organization organization = new Organization("org");
      organizationDAO.insert(organization);

      Application application = new Application("app", "app", organization.getPublicId());
      applicationDAO.insert(application);

      Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
      constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));

      Policy policy1 = new Policy("policyId1", "policy1");
      policy1.setOwnerId(organization.getId());
      policy1.addConstraint(constraint);
      policyDAO.insert(policy1);
      Policy policy2 = new Policy("policyId2", "policy2");
      policy2.setOwnerId(organization.getId());
      policy2.addConstraint(constraint);
      policyDAO.insert(policy2);
      Policy policy3 = new Policy("policyId3", "policy3");
      policy3.setOwnerId(organization.getId());
      policy3.addConstraint(constraint);
      policyDAO.insert(policy3);

      policyWaiverDAO.insert(new PolicyWaiver(policy1.getId(), application.getId(), "comment1"));
      policyWaiverDAO.insert(new PolicyWaiver(policy2.getId(), application.getId(), "comment2"));
      policyWaiverDAO.insert(new PolicyWaiver(policy3.getId(), application.getId(), "comment3"));

      assertThat(telemetryCollector.collectData().getAttributes())
          .containsEntry(PolicyStatusOverrideTelemetryCollector.POLICY_WAIVER_COUNT, "3");
    });

    testAsNewTenant(t2 -> {
      assertThat(telemetryCollector.collectData().getAttributes())
          .containsEntry(PolicyStatusOverrideTelemetryCollector.POLICY_WAIVER_COUNT, "0");
    });
  }
}
