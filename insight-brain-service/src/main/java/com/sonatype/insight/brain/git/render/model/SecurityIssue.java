/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render.model;

import static com.google.common.base.Preconditions.checkNotNull;

public class SecurityIssue
{
  private final int threatLevel;

  private final SeverityInfo severityInfo;

  private final String description;

  private final String policyViolationDetailsLink;

  public SecurityIssue(
      final int threatLevel,
      final SeverityInfo severityInfo,
      final String description,
      final String policyViolationDetailsLink)
  {
    this.threatLevel = threatLevel;
    this.severityInfo = severityInfo; // nullable in-case its not Vulnerability related
    this.description = description;
    this.policyViolationDetailsLink = checkDefined(policyViolationDetailsLink, "policyViolationDetailsLink");
  }

  public String getDescription() {
    return description;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public String getPolicyViolationDetailsLink() {
    return policyViolationDetailsLink;
  }

  public SeverityInfo getSeverityInfo() {
    return severityInfo;
  }

  private static <T> T checkDefined(final T value, final String variableName) {
    return checkNotNull(value, variableName + " is required and cannot be null");
  }
}
