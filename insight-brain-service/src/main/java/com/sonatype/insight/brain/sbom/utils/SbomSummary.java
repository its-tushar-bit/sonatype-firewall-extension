/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

public class SbomSummary
{
  public String specification;

  public String format;

  public String version;

  public Integer componentCount;

  public Integer vulnerabilityCount;

  public String applicationName;

  /**
   * The version detected from the SBOM file. Not necessarily the version saved in `sbom_metadata.sbom_version`.
   */
  public String applicationVersion;

  public String serialNumber;

  public String creationDetails;
}
