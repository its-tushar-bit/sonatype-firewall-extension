/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

/**
 * @deprecated The related API endpoint is deprecated. To be removed when the Repository Results View migration to
 * React is completed (Epic: https://issues.sonatype.org/browse/CLM-20597)
 */
@Deprecated
public class RepositoryReportDetail
{
  private final ComponentIdentifier componentIdentifier;

  private final String componentDisplayText;

  private final String pathname;

  private final String hash;

  private final String matchState;

  private final boolean quarantined;

  private final boolean waived;

  private final int threatLevel;

  private final boolean highestThreatLevel;

  private final String policyName;

  public static RepositoryReportDetail create(final RepositoryComponent component) {
    return create(component, null, true);
  }

  public static RepositoryReportDetail create(final RepositoryComponent component,
                                              final RepositoryPolicyViolation violation,
                                              final boolean highestThreatLevel)
  {

    final String componentDisplayText = buildComponentDisplayText(component);

    if (violation == null) {
      return new RepositoryReportDetail(component.getComponentIdentifier(), componentDisplayText,
          component.getPathname(), component.getHash(), component.getMatchStateId(), component.isQuarantined(), false,
          0, highestThreatLevel, null);
    }
    else {
      return new RepositoryReportDetail(component.getComponentIdentifier(), componentDisplayText,
          component.getPathname(), component.getHash(), component.getMatchStateId(), component.isQuarantined(),
          violation.isWaived(), violation.getThreatLevel(), highestThreatLevel, violation.getPolicyName());
    }
  }

  static String buildComponentDisplayText(final RepositoryComponent component) {
    final ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    if (componentIdentifier != null) {
      return ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
    }

    String pathname = component.getPathname();
    if (pathname == null) {
      return null;
    }
    return pathname.substring(pathname.lastIndexOf('/') + 1) + " (" + pathname + ")";
  }

  public RepositoryReportDetail() {
    this(null, null, null, null, null, false, false, 0, false, null);
  }

  private RepositoryReportDetail(final ComponentIdentifier componentIdentifier,
                                 final String componentDisplayText,
                                 final String pathname,
                                 final String hash,
                                 final String matchStateId,
                                 final boolean quarantined,
                                 final boolean waived,
                                 final int threatLevel,
                                 final boolean highestThreatLevel,
                                 final String policyName)
  {
    this.componentIdentifier = componentIdentifier;
    this.componentDisplayText = componentDisplayText;
    this.pathname = pathname;
    this.hash = hash;
    this.matchState = matchStateId;
    this.waived = waived;
    this.quarantined = quarantined;
    this.threatLevel = threatLevel;
    this.highestThreatLevel = highestThreatLevel;
    this.policyName = policyName;
  }

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  public String getComponentDisplayText() {
    return componentDisplayText;
  }

  public String getPathname() {
    return pathname;
  }

  public String getHash() {
    return hash;
  }

  public String getMatchState() {
    return matchState;
  }

  public boolean isWaived() {
    return waived;
  }

  public boolean isQuarantined() {
    return quarantined;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public boolean isHighestThreatLevel() {
    return highestThreatLevel;
  }

  public String getPolicyName() {
    return policyName;
  }

  @Override
  public String toString() {
    return "RepositoryReportDetail{" + "componentIdentifier=" + componentIdentifier + ", componentDisplayText="
        + componentDisplayText + ", pathname='" + pathname + '\'' + ", hash='" + hash + '\'' + ", matchState='"
        + matchState + '\'' + ", quarantined=" + quarantined + ", waived=" + waived + ", threatLevel=" + threatLevel
        + ", highestThreatLevel=" + highestThreatLevel + ", policyName='" + policyName + '\'' + '}';
  }
}
