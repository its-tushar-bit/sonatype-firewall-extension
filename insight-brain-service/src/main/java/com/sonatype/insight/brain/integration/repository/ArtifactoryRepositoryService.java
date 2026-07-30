/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.container.images.ContainerImageReportService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.RequestSafeComponentsMetricEventService;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
public class ArtifactoryRepositoryService
    extends AbstractRepositoryService
{
  @Inject
  public ArtifactoryRepositoryService(
      RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      ProprietaryComponentNameDetector proprietaryComponentNameDetector,
      ProductLicense productLicense,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator,
      DbQuarantinedComponentAccessManager quarantinedComponentAccessManager,
      FirewallQuarantineHdsClient quarantineHdsClient,
      ApplicationDAO applicationDAO,
      ApplicationService applicationService,
      TelemetrySender telemetrySender,
      RepositoryManagerDAO repositoryManagerDAO,
      RepositoryDAO repositoryDAO,
      ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      FirewallIgnorePatternService firewallIgnorePatternService,
      RequestSafeComponentsMetricEventService requestSafeComponentsMetricEventService,
      com.sonatype.insight.brain.repository.RepositoryService repositoryService,
      ContainerImageReportService containerImageReportService)
  {
    super(repositoryPolicyEvaluator, proprietaryComponentNameDetector, productLicense, policyViolationLoggerFactory,
        LicensedFeature.FIREWALL_FOR_ARTIFACTORY, proxyRepositoryComponentTelemetryCreator,
        quarantinedComponentAccessManager, quarantineHdsClient, applicationDAO, applicationService, telemetrySender,
        repositoryManagerDAO, repositoryDAO, proxyRepositoryComponentDAO, proxyRepositoryPolicyViolationDAO,
        firewallIgnorePatternService, requestSafeComponentsMetricEventService, repositoryService,
        containerImageReportService);
  }
}
