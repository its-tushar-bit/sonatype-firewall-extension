/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
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
  private final SamlConfigurationDAO samlConfigurationDAO;

  public static final String SAML_CONFIGURED = "saml_configured";

  @Inject
  public RealmTelemetryCollector(SamlConfigurationDAO samlConfigurationDAO) {
    this.samlConfigurationDAO = samlConfigurationDAO;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REALM);
    telemetryData.getAttributes().put(SAML_CONFIGURED, String.valueOf(samlConfigurationDAO.get() != null));
    return telemetryData;
  }
}
