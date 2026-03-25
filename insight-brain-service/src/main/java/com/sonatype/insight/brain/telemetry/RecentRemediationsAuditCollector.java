/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.Date;
import java.util.List;
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
 * Audit collector that reports policy violations remediated in the last 24-48 hours.
 */
@Named
@Singleton
public class RecentRemediationsAuditCollector
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
  public RecentRemediationsAuditCollector(
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
    List<PolicyViolation> remediatedViolations = policyViolationDAO.findRemediatedSince(cutoffTime);

    policyViolationDAO.loadConstraintFacts(remediatedViolations);

    return remediatedViolations.stream()
        .map(this::createTelemetryForRemediation)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  /**
   * Creates telemetry for a remediated violation. Returns all telemetry entries including
   * TIME_TO_REMEDIATE_POLICY_VIOLATION and, if applicable, TIME_TO_CHANGE_VERSION_POLICY_VIOLATION
   * for violations fixed by version upgrades/downgrades.
   */
  private List<TelemetryData> createTelemetryForRemediation(PolicyViolation violation) {
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(
        policyWaiverDAO,
        sourceControlEventDAO,
        telemetryUtils,
        licenseNameProvider,
        SCM_DISABLED,
        componentHelper);

    telemetryCollector.setTimeOfPolicyEvaluation(violation.getFixTime());

    Component component = new Component();
    component.setComponentIdentifier(violation.getComponentIdentifier());
    telemetryCollector.addTelemetryForFixedViolation(violation, Collections.singletonList(component));
    List<TelemetryData> telemetryDataList = telemetryCollector.getTelemetryData();

    if (telemetryDataList.isEmpty()) {
      return Collections.emptyList();
    }

    // Mark all telemetry entries as audit telemetry (includes both TTRPV and TTCVPV if present)
    return telemetryDataList.stream()
        .map(data -> {
          data.getAttributes().put(AUDIT_TELEMETRY_ATTRIBUTE, true);
          return data;
        })
        .collect(Collectors.toList());
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
