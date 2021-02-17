/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.experimental.dto.FirewallConfigurationDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;

/**
 * @since 1.106.0
 */
@Named
public class ApiFirewallService
{
  private final InsightConfig insightConfig;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final ProductLicense productLicense;

  private final RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  public ApiFirewallService(
      final InsightConfig insightConfig,
      final PolicyMonitoringDAO policyMonitoringDAO,
      final ProductLicense productLicense,
      final RepositoryComponentDAO repositoryComponentDAO)
  {
    this.insightConfig = insightConfig;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.productLicense = productLicense;
    this.repositoryComponentDAO = repositoryComponentDAO;
  }

  @Authorize(permission = Permission.READ)
  public FirewallConfigurationDTO getFirewallConfiguration() {
    checkExperimentalFeatureFlag();

    checkProductLicense();

    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled =
        null != policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    return firewallConfigurationDTO;
  }

  @Authorize(permission = Permission.WRITE)
  public FirewallConfigurationDTO setFirewallConfiguration(final FirewallConfigurationDTO firewallConfigurationDTO) {
    checkExperimentalFeatureFlag();

    checkProductLicense();

    final PolicyMonitoring existingPolicyMonitoring = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    if (null != existingPolicyMonitoring && !firewallConfigurationDTO.autoUnquarantineEnabled) {
      executeWithAuditSession(() -> policyMonitoringDAO.delete(existingPolicyMonitoring));
    }
    else if (null == existingPolicyMonitoring && firewallConfigurationDTO.autoUnquarantineEnabled) {
      PolicyMonitoring policyMonitoring = new PolicyMonitoring();
      policyMonitoring.setStageTypeId(StageTypes.PROXY.getId());
      policyMonitoring.setOwnerId(REPOSITORY_CONTAINER_ID);
      executeWithAuditSession(() -> policyMonitoringDAO.insert(policyMonitoring));
    }
    return getFirewallConfiguration();
  }

  private void executeWithAuditSession(Runnable runnable) {
    try (AuditSession auditSession = AuditData.get()
        .recordSubEvent(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, false)) {
      AuditData.get().setOwner(new OwnerDAO().getById(REPOSITORY_CONTAINER_ID)).setStageId(StageTypes.PROXY.getId());
      runnable.run();
    }
  }

  private void checkExperimentalFeatureFlag() {
    if (!insightConfig.isExperimentalFeatureEnabled(Feature.FIREWALL_AUTO_UNQUARANTINE)) {
      throw new BadRequestException("Firewall experimental feature is not enabled.");
    }
  }

  private void checkProductLicense() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL) ||
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY)) {
      throw new InvalidLicenseException();
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiFirewallReleaseQuarantineSummaryDTO getReleaseQuarantineSummary() {
    checkExperimentalFeatureFlag();
    checkProductLicense();

    final Date startOfCurMonth =
        Date.from((LocalDate.now().withDayOfMonth(1)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

    final ApiFirewallReleaseQuarantineSummaryDTO
        apiFirewallReleaseQuarantineSummaryDTO = new ApiFirewallReleaseQuarantineSummaryDTO();

    apiFirewallReleaseQuarantineSummaryDTO.autoReleaseQuarantineCountMTD =
        repositoryComponentDAO.getAutoReleaseQuarantinedCountByDate(startOfCurMonth);

    return apiFirewallReleaseQuarantineSummaryDTO;
  }
}
