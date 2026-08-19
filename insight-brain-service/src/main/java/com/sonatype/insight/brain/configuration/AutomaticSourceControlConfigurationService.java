/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.77
 */
@Named
public class AutomaticSourceControlConfigurationService
{
  static final String AUTO_SCM_CONFIGURATION_ENABLED_TELEMETRY_ATTR = "automatic_source_control_configuration_enabled";

  private final AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  private final TelemetrySender telemetrySender;

  @Inject
  public AutomaticSourceControlConfigurationService(
      AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO,
      TelemetrySender telemetrySender)
  {
    this.automaticSourceControlConfigurationDAO = automaticSourceControlConfigurationDAO;
    this.telemetrySender = telemetrySender;
  }

  private void persistConfiguration(AutomaticSourceControlConfiguration configuration) {
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(
        configuration.isEnabled());
    auditAutomaticSourceControlConfiguration(configuration);
  }

  private void auditAutomaticSourceControlConfiguration(AutomaticSourceControlConfiguration configuration) {
    if (configuration.isEnabled()) {
      AuditData.get().setData("automaticSourceControlConfiguration", "enabled");
    }
    else {
      AuditData.get().setData("automaticSourceControlConfiguration", "disabled");
    }
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  AutomaticSourceControlConfiguration update(AutomaticSourceControlConfiguration configuration) {

    boolean wasEnabledBefore = automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled();

    persistConfiguration(configuration);

    sendTelemetryEvent(wasEnabledBefore, configuration.isEnabled());

    return configuration;
  }

  /**
   * Sends a telemetry event if the Automatic SCM was enabled or disabled.
   */
  private void sendTelemetryEvent(boolean wasEnabledBefore, boolean isEnabledNow) {
    if (wasEnabledBefore == isEnabledNow) {
      return;
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTOMATIC_ONBOARDING);
    telemetryData.getAttributes().put(AUTO_SCM_CONFIGURATION_ENABLED_TELEMETRY_ATTR, String.valueOf(isEnabledNow));
    telemetrySender.send(telemetryData);
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  AutomaticSourceControlConfiguration get() {
    return new AutomaticSourceControlConfiguration(
        automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled());
  }
}
