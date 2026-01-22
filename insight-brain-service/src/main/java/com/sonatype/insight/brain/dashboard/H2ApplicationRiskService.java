/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;

@Named
public class H2ApplicationRiskService
    extends AbstractApplicationRiskService
{
  @Inject
  public H2ApplicationRiskService(
      final ApplicationService applicationService,
      final OrganizationDAO organizationDAO,
      final PolicyViolationLoader policyViolationLoader,
      final DashboardUtils dashboardUtils,
      final AuditService auditService)
  {
    super(applicationService, organizationDAO, policyViolationLoader, dashboardUtils, auditService);
  }
}
