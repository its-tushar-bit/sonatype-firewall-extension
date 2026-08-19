/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

/**
 * Per-policy summary of open (unfixed, unwaived) violations, used by dashboard-tile aggregations (e.g. the
 * Legal Obligations tile's non-ALP "top legal violations" payload, CLM-39604 / P1.5-D-2).
 *
 * <p>
 * Naming follows {@link com.sonatype.insight.brain.model.license.LicenseThreatGroupCount}: the class names the
 * entity (policy + its violation summary), with the metric carried in {@link #openViolationCount}.
 *
 * @since 1.205
 */
public class PolicyOpenViolationSummary
{
  private String policyId;

  private String policyName;

  private long openViolationCount;

  public PolicyOpenViolationSummary() {
  }

  public PolicyOpenViolationSummary(String policyId, String policyName, long openViolationCount) {
    this.policyId = policyId;
    this.policyName = policyName;
    this.openViolationCount = openViolationCount;
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  public String getPolicyName() {
    return policyName;
  }

  public void setPolicyName(String policyName) {
    this.policyName = policyName;
  }

  public long getOpenViolationCount() {
    return openViolationCount;
  }

  public void setOpenViolationCount(long openViolationCount) {
    this.openViolationCount = openViolationCount;
  }
}
