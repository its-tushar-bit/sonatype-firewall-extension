/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

public class SbomImportMetricsTelemetry
{
  public static final String ATTRIBUTE_NAME = "sbom_import_metrics";

  private int verifiedVulnerabilityCount;

  private int unverifiedVulnerabilityCount;

  public SbomImportMetricsTelemetry() {
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
}
