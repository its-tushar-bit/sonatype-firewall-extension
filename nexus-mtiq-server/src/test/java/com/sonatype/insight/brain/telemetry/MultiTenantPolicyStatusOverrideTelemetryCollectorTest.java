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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.TenantManagerTestHelper.setTestTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantPolicyStatusOverrideTelemetryCollectorTest
    extends MultiTenantTelemetryCollectorTest
{
  private PolicyStatusOverrideTelemetryCollector telemetryCollector;

  private PolicyWaiverDAO policyWaiverDAO;

  private OrganizationDAO organizationDAO;

  private ApplicationDAO applicationDAO;

  private PolicyDAO policyDAO;

  @Before
  public void setup() {
    SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO = new SecurityVulnerabilityOverrideDAO();
    policyDAO = new PolicyDAO();
    policyWaiverDAO = new PolicyWaiverDAO();
    organizationDAO = new OrganizationDAO();
    applicationDAO = new ApplicationDAO();
    telemetryCollector = new PolicyStatusOverrideTelemetryCollector(securityVulnerabilityOverrideDAO, policyWaiverDAO);
  }

  @Test
  public void testShouldNotLeakDataBetweenTenants_whenMultiTenantMode() {
    Tenant tenant1 = new Tenant("tenant1");
    Tenant tenant2 = new Tenant("tenant2");

    testAs(tenant1, t1 -> {
      setTestTenant(tenantManager, tenant1);
    });

    testAs(tenant2, t2 -> {
      setTestTenant(tenantManager, tenant2);
    });

    testAs(tenant1, t1 -> {
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

    testAs(tenant2, t2 -> {
      assertThat(telemetryCollector.collectData().getAttributes())
          .containsEntry(PolicyStatusOverrideTelemetryCollector.POLICY_WAIVER_COUNT, "0");
    });
  }
}
