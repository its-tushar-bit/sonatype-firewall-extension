/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.callflow;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.brain.report.ReportService;

@Named
public class ComponentReachabilityService
{
  private final ReportService reportService;

  @Inject
  public ComponentReachabilityService(final ReportService reportService) {
    this.reportService = reportService;
  }

  public ReachabilityStatus isComponentReachable(
      final String applicationPublicId,
      final String scanId,
      final String componentHash)
  {
    final PolicyThreats policyThreats = reportService.getPolicyThreats(applicationPublicId, scanId);
    return isComponentReachable(policyThreats, componentHash);
  }

  public ReachabilityStatus isComponentReachable(
      final PolicyThreats policyThreats,
      final String componentHash)
  {
    return hasReachableSecurityViolation(policyThreats.aaData, componentHash);
  }

  private ReachabilityStatus hasReachableSecurityViolation(
      final List<PolicyThreats.Component> policyThreatsComponents,
      final String componentHash)
  {
    Set<ReachabilityStatus> reachabilityStatuses = new HashSet<>();
    for (Component policyThreatsComponent : policyThreatsComponents) {
      if (policyThreatsComponent.hash.equals(componentHash)) {
        for (PolicyViolation activeViolation : policyThreatsComponent.activeViolations) {
          boolean reachabilitySupported = PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(
              policyThreatsComponent.componentIdentifier,
              activeViolation);
          if (reachabilitySupported && !activeViolation.legacyViolation) {
            reachabilityStatuses.add(activeViolation.reachabilityStatus);
          }
        }
      }
    }
    return ReachabilityStatus.combine(reachabilityStatuses.stream());
  }
}
