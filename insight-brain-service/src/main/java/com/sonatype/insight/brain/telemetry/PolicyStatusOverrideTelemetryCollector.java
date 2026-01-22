/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
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

  private final PolicyWaiverDAO policyWaiverDAO;

  public static final String SECURITY_VULNERABILITY_OVERRIDE_COUNT = "security_vulnerability_override_count";

  public static final String POLICY_WAIVER_COUNT = "policy_waiver_count";

  @Inject
  public PolicyStatusOverrideTelemetryCollector(SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
                                                PolicyWaiverDAO policyWaiverDAO)
  {
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.policyWaiverDAO = policyWaiverDAO;
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.POLICY_STATUS_OVERRIDE);
    Map<String, Object> attributes = telemetryData.getAttributes();
    attributes.put(SECURITY_VULNERABILITY_OVERRIDE_COUNT, String.valueOf(securityVulnerabilityOverrideDAO.getCount()));
    attributes.put(POLICY_WAIVER_COUNT, String.valueOf(policyWaiverDAO.getCount()));
    return telemetryData;
  }
}
