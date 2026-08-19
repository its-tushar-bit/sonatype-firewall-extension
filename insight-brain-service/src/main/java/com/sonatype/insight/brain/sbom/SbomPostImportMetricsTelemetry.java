/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.io.Serializable;

public class SbomPostImportMetricsTelemetry
    implements Serializable
{
  public static final String ATTRIBUTE_NAME = "sbom_post_import_metrics";

  private int verifiedVulnerabilityCount;

  private int unverifiedVulnerabilityCount;

  private int additionalVulnerabilitiesCount;

  private int totalVulnerabilitiesCount;

  public SbomPostImportMetricsTelemetry() {
  }

  public int getVerifiedVulnerabilityCount() {
    return verifiedVulnerabilityCount;
  }

  public void incrementVerifiedVulnerabilityCount() {
    this.verifiedVulnerabilityCount++;
  }

  public int getUnverifiedVulnerabilityCount() {
    return unverifiedVulnerabilityCount;
  }

  public void incrementUnverifiedVulnerabilityCount() {
    this.unverifiedVulnerabilityCount++;
  }

  public int getAdditionalVulnerabilitiesCount() {
    return additionalVulnerabilitiesCount;
  }

  public void incrementAdditionalVulnerabilitiesCount() {
    this.additionalVulnerabilitiesCount++;
  }

  public int getTotalVulnerabilitiesCount() {
    return totalVulnerabilitiesCount;
  }

  public void addToTotalVulnerabilitiesCount(final int vulnerabilitiesCount) {
    this.totalVulnerabilitiesCount += vulnerabilitiesCount;
  }

  public void addToUnverifiedVulnerabilityCount(final int unverifiedVulnerabilityCount) {
    this.unverifiedVulnerabilityCount += unverifiedVulnerabilityCount;
  }

  public void reset() {
    this.verifiedVulnerabilityCount = 0;
    this.unverifiedVulnerabilityCount = 0;
    this.additionalVulnerabilitiesCount = 0;
    this.totalVulnerabilitiesCount = 0;
  }
}
