/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Map;
import java.util.Set;

import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * Represents a set of Purl identifiers with vulnerabilities for a given application and scan.
 */
public class PurlIdentifiersWithVulnerabilities
{
  private final String applicationId;

  private final String scanId;

  private final Map<PackageUrlIdentifier, Set<String>> vulnerabilitiesByPurlIdentifiers;

  public PurlIdentifiersWithVulnerabilities(
      final String applicationId,
      final String scanId,
      final Map<PackageUrlIdentifier, Set<String>> vulnerabilitiesByPurlIdentifiers)
  {
    this.applicationId = applicationId;
    this.scanId = scanId;
    this.vulnerabilitiesByPurlIdentifiers = vulnerabilitiesByPurlIdentifiers;
  }

  public Map<PackageUrlIdentifier, Set<String>> getVulnerabilitiesByPurlIdentifiers() {
    return vulnerabilitiesByPurlIdentifiers;
  }

  public String getScanId() {
    return scanId;
  }

  public String getApplicationId() {
    return applicationId;
  }
}
