/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.52
 */
@Named
@Singleton
public class PolicyStatusOverrideTelemetryCollector
    implements TelemetryCollector
{
  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  public static final String SECURITY_VULNERABILITY_OVERRIDE_COUNT = "security_vulnerability_override_count";

  @Inject
  public PolicyStatusOverrideTelemetryCollector(SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO) {
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.POLICY_STATUS_OVERRIDE);
    Map<String, Object> attributes = telemetryData.getAttributes();
    attributes.put(SECURITY_VULNERABILITY_OVERRIDE_COUNT, String.valueOf(securityVulnerabilityOverrideDAO.getCount()));
    return telemetryData;
  }

}
