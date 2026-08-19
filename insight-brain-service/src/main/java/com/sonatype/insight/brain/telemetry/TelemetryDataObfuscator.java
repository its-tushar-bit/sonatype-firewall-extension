/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.service.Configuration;

// This class could hold the list of attributes that are meant to be obfuscated, so it could be used
// in a whole TelemetryData object. Might not be the most efficient way for some logic already in place or when
// Streams are being used to populate the TelemetryData
@Named
@Singleton
public class TelemetryDataObfuscator
{
  private final Configuration configuration;

  @Inject
  public TelemetryDataObfuscator(final Configuration configuration) {
    this.configuration = configuration;
  }

  String obfuscate(String value) {
    return HdsClientAnalytics.obfuscate(value);
  }

  String obfuscateIfAdvancedReportingDisabled(String value) {
    if (!configuration.getAdvanceReportingInsightsEnabled()) {
      return obfuscate(value);
    }
    return value;
  }
}
