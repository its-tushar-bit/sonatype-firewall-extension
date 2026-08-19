/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.component.ComponentHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.joda.time.DateTime;

/**
 * Audit collector that reports policy violations waived in the last 24-48 hours.
 */
@Named
@Singleton
public class RecentWaiversAuditCollector
    implements TelemetryCollector
{
  private static final int LOOKBACK_HOURS = 48;

  private static final boolean SCM_DISABLED = false;

  private static final String AUDIT_TELEMETRY_ATTRIBUTE = "is_audit_telemetry";

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final TelemetryUtils telemetryUtils;

  private final LicenseNameProvider licenseNameProvider;

  private final ComponentHelper componentHelper;

  @Inject
  public RecentWaiversAuditCollector(
      PolicyViolationDAO policyViolationDAO,
      PolicyWaiverDAO policyWaiverDAO,
      SourceControlEventDAO sourceControlEventDAO,
      TelemetryUtils telemetryUtils,
      LicenseNameProvider licenseNameProvider,
      ComponentHelper componentHelper)
  {
    this.policyViolationDAO = policyViolationDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.telemetryUtils = telemetryUtils;
    this.licenseNameProvider = licenseNameProvider;
    this.componentHelper = componentHelper;
  }

  @Override
  public List<TelemetryData> collectAllData() {
    Date cutoffTime = DateTime.now().minusHours(LOOKBACK_HOURS).toDate();
    List<PolicyViolation> waivedViolations = policyViolationDAO.findCurrentlyWaivedSince(cutoffTime);

    // Load constraint facts for all violations (required for telemetry creation)
    policyViolationDAO.loadConstraintFacts(waivedViolations);

    return waivedViolations.stream()
        .map(this::createTelemetryForWaiver)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Creates telemetry for a waived violation.
   * Supports both manual waivers (policy_waiver_id) and auto waivers (auto_policy_waiver_id).
   *
   * @param violation the waived violation
   * @return telemetry data with is_audit_telemetry=true, or null if no telemetry generated
   */
  private TelemetryData createTelemetryForWaiver(PolicyViolation violation) {
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(
        policyWaiverDAO,
        sourceControlEventDAO,
        telemetryUtils,
        licenseNameProvider,
        SCM_DISABLED,
        componentHelper);

    telemetryCollector.setTimeOfPolicyEvaluation(violation.getWaiveTime());

    Component component = new Component();
    component.setComponentIdentifier(violation.getComponentIdentifier());

    if (violation.isAutoWaived()) {
      telemetryCollector.addTelemetryForAutoWaivedViolation(violation, component);
    }
    else {
      telemetryCollector.addTelemetryForWaivedViolation(violation, component);
    }

    List<TelemetryData> telemetryDataList = telemetryCollector.getTelemetryData();

    if (telemetryDataList.isEmpty()) {
      return null;
    }

    TelemetryData data = telemetryDataList.get(0);
    data.getAttributes().put(AUDIT_TELEMETRY_ATTRIBUTE, true);
    return data;
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
