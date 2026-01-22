/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.75
 */
@Named
@Singleton
public class RealmTelemetryCollector
    implements TelemetryCollector
{
  private final SamlConfigurationService samlConfigurationService;

  public static final String SAML_CONFIGURED = "saml_configured";

  @Inject
  public RealmTelemetryCollector(SamlConfigurationService samlConfigurationService) {
    this.samlConfigurationService = samlConfigurationService;
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REALM);
    telemetryData.getAttributes().put(SAML_CONFIGURED, String.valueOf(samlConfigurationService.get() != null));
    return telemetryData;
  }
}
