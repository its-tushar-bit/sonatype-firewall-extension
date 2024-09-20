/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.callflow;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ReportService;

import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.REACHABLE;

@Named
public class ComponentReachabilityService
{
  private final ReportService reportService;

  @Inject
  public ComponentReachabilityService(final ReportService reportService) {
    this.reportService = reportService;
  }

  public boolean isComponentReachable(
      final String applicationPublicId,
      final String scanId,
      final String componentHash)
  {
    final PolicyThreats policyThreats = reportService.getPolicyThreats(applicationPublicId, scanId);
    return hasReachableViolation(policyThreats.aaData, componentHash);
  }

  private boolean hasReachableViolation(
      final List<PolicyThreats.Component> policyThreatsComponents,
      final String componentHash)
  {
    return policyThreatsComponents.stream()
        .filter(policyThreatComponent -> policyThreatComponent.hash.equals(componentHash))
        .map(comp -> comp.activeViolations)
        .flatMap(Collection::stream)
        .filter(violation -> !violation.legacyViolation)
        .anyMatch(policyViolation -> REACHABLE.equals(policyViolation.reachabilityStatus));
  }
}
