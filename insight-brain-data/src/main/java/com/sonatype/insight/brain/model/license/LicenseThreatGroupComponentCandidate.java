/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;

/**
 * A scoped application component whose effective license maps to a license threat group.
 *
 * @since 1.204
 */
public class LicenseThreatGroupComponentCandidate
{
  private final String licenseThreatGroupId;

  private final String licenseThreatGroupName;

  private final int threatLevel;

  private final String applicationId;

  private final String hash;

  private final String componentIdFormat;

  private final String componentIdCoordinatesJson;

  private final String effectiveLicenseId;

  public LicenseThreatGroupComponentCandidate(
      String licenseThreatGroupId,
      String licenseThreatGroupName,
      int threatLevel,
      String applicationId,
      String hash,
      String componentIdFormat,
      String componentIdCoordinatesJson,
      String effectiveLicenseId)
  {
    this.licenseThreatGroupId = licenseThreatGroupId;
    this.licenseThreatGroupName = licenseThreatGroupName;
    this.threatLevel = threatLevel;
    this.applicationId = applicationId;
    this.hash = hash;
    this.componentIdFormat = componentIdFormat;
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
    this.effectiveLicenseId = effectiveLicenseId;
  }

  public String getLicenseThreatGroupId() {
    return licenseThreatGroupId;
  }

  public String getLicenseThreatGroupName() {
    return licenseThreatGroupName;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public String getHash() {
    return hash;
  }

  public String getComponentIdFormat() {
    return componentIdFormat;
  }

  public String getComponentIdCoordinatesJson() {
    return componentIdCoordinatesJson;
  }

  public String getEffectiveLicenseId() {
    return effectiveLicenseId;
  }

  public ComponentIdentifier getComponentIdentifier() {
    return ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(componentIdFormat,
        componentIdCoordinatesJson);
  }
}
