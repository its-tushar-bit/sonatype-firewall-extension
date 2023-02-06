/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.TenantManagerTestHelper.setTestTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantHierarchyMetricsTelemetryCollectorTest
    extends MultiTenantTelemetryCollectorTest
{
  private HierarchyMetricsTelemetryCollector telemetryCollector;

  private ApplicationDAO applicationDAO;

  private OrganizationDAO organizationDAO;

  @Before
  public void setup() {
    organizationDAO = new OrganizationDAO();
    applicationDAO = new ApplicationDAO();
    RepositoryDAO repositoryDAO = new RepositoryDAO();
    RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();
    ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();
    RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    telemetryCollector = new HierarchyMetricsTelemetryCollector(applicationDAO, organizationDAO, repositoryDAO,
        repositoryComponentDAO, applicationComponentDAO, repositoryPolicyViolationDAO, policyViolationDAO,
        policyEvaluationDAO);
  }

  @Test
  public void testShouldNotLeakDataBetweenTenants_whenMultiTenantMode() {
    Tenant tenant1 = createTenant("tenant1");
    Tenant tenant2 = createTenant("tenant2");

    setTenant(tenant1);
    setTestTenant(tenantManager, tenant1);
    createAppsAndOrgs(2);

    setTenant(tenant2);
    setTestTenant(tenantManager, tenant2);

    testAs(tenant2, t2 -> {
      TelemetryData telemetryData = telemetryCollector.collectData();
      assertThat(telemetryData.getAttributes())
          .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "0")
          .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "0")
          .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "0")
          .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
          .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "0");
    });

    testAs(tenant1, t1 -> {
      TelemetryData telemetryData = telemetryCollector.collectData();
      assertThat(telemetryData.getAttributes())
          .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "2")
          .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "1")
          .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "1")
          .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
          .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "1.0");
    });
  }

  private void createAppsAndOrgs(int numberOfOrgs) {
    for (int i = 0; i < numberOfOrgs; i++) {
      Organization organization = new Organization(String.format("org%d", i));
      organizationDAO.insert(organization);

      for (int j = 0; j < i; j++) {
        String appName = String.format("app%d", j);
        Application application = new Application(appName, appName, organization.getPublicId());
        applicationDAO.insert(application);
      }
    }
  }
}
