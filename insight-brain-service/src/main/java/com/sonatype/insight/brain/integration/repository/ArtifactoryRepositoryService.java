/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.RequestSafeComponentsMetricEventService;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
public class ArtifactoryRepositoryService extends AbstractRepositoryService
{
  @Inject
  public ArtifactoryRepositoryService(
      RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      ProprietaryComponentNameDetector proprietaryComponentNameDetector,
      ProductLicense productLicense,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator,
      DbQuarantinedComponentAccessManager quarantinedComponentAccessManager,
      FirewallQuarantineHdsClient quarantineHdsClient,
      TelemetrySender telemetrySender,
      RepositoryManagerDAO repositoryManagerDAO,
      RepositoryDAO repositoryDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      FirewallIgnorePatternService firewallIgnorePatternService,
      RequestSafeComponentsMetricEventService requestSafeComponentsMetricEventService)
  {
    super(repositoryPolicyEvaluator, proprietaryComponentNameDetector, productLicense, policyViolationLoggerFactory,
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY, repositoryComponentTelemetryCreator,
        quarantinedComponentAccessManager, quarantineHdsClient, telemetrySender, repositoryManagerDAO, repositoryDAO,
        repositoryComponentDAO, repositoryPolicyViolationDAO,
        firewallIgnorePatternService, requestSafeComponentsMetricEventService);
  }
}
