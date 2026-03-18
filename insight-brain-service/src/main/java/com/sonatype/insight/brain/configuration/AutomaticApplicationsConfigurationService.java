/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.43
 */
@Named
public class AutomaticApplicationsConfigurationService
{
  static final String AUTO_APP_CREATION_ENABLED_TELEMETRY_ATTR = "automatic_application_creation_enabled";

  private final OrganizationDAO organizationDAO;

  private final AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  private final TelemetrySender telemetrySender;

  @Inject
  public AutomaticApplicationsConfigurationService(
      OrganizationDAO organizationDAO,
      AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO,
      TelemetrySender telemetrySender)
  {
    this.organizationDAO = organizationDAO;
    this.automaticApplicationsConfigurationDAO = automaticApplicationsConfigurationDAO;
    this.telemetrySender = telemetrySender;
  }

  private void validateConfiguration(AutomaticApplicationsConfiguration configuration) {
    String parentOrganizationId = StringUtils.trimToEmpty(configuration.getParentOrganizationId());
    if (Organization.ROOT_ORGANIZATION_ID.equals(parentOrganizationId)) {
      throw new BadRequestException("Parent cannot be the root organization.");
    }
    if (configuration.isEnabled() && StringUtils.isBlank(parentOrganizationId)) {
      throw new BadRequestException(
          "Parent organization ID is required when automatic application creation is enabled.");
    }
    if (StringUtils.isNotBlank(parentOrganizationId) && organizationDAO.getById(parentOrganizationId) == null) {
      throw new BadRequestException("Parent organization ID " + parentOrganizationId + " not found.");
    }
    configuration.setParentOrganizationId(parentOrganizationId);
  }

  private void persistConfiguration(AutomaticApplicationsConfiguration configuration) {
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      automaticApplicationsConfigurationDAO.setEnabled(tx, configuration.isEnabled());
      automaticApplicationsConfigurationDAO.setOrganizationId(tx, configuration.getParentOrganizationId());
      tx.commit();
      auditAutomaticApplicationConfiguration(configuration);
    }
  }

  private void auditAutomaticApplicationConfiguration(AutomaticApplicationsConfiguration configuration) {
    if (configuration.isEnabled()) {
      AuditData.get()
          .setData("automaticApplicationCreation", "enabled")
          .setParentOrganization(organizationDAO.getByIdNotNull(configuration.getParentOrganizationId()));
    }
    else {
      AuditData.get().setData("automaticApplicationCreation", "disabled");
    }
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION)
  AutomaticApplicationsConfiguration update(AutomaticApplicationsConfiguration configuration) {
    validateConfiguration(configuration);

    boolean wasEnabledBefore = automaticApplicationsConfigurationDAO.isEnabled();

    persistConfiguration(configuration);

    sendTelemetryEvent(wasEnabledBefore, configuration.isEnabled());

    return configuration;
  }

  /**
   * Sends a telemetry event if the Automatic App Creation was enabled or disabled.
   */
  private void sendTelemetryEvent(boolean wasEnabledBefore, boolean isEnabledNow) {
    if (wasEnabledBefore == isEnabledNow) {
      return;
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION);
    telemetryData.getAttributes().put(AUTO_APP_CREATION_ENABLED_TELEMETRY_ATTR, String.valueOf(isEnabledNow));

    telemetrySender.send(telemetryData);
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION)
  AutomaticApplicationsConfiguration get() {
    return new AutomaticApplicationsConfiguration(automaticApplicationsConfigurationDAO.isEnabled(),
        automaticApplicationsConfigurationDAO.getOrganizationId());
  }
}
