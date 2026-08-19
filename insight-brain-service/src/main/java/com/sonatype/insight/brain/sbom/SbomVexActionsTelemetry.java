/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom;

public class SbomVexActionsTelemetry
{
  private String actionType;

  private String componentPackageUrl;

  private String componentHash;

  private String vexState;

  public SbomVexActionsTelemetry(
      String actionType,
      String componentPackageUrl,
      String componentHash,
      String vexState)
  {
    this.actionType = actionType;
    this.componentPackageUrl = componentPackageUrl;
    this.componentHash = componentHash;
    this.vexState = vexState;
  }

  public String getActionType() {
    return actionType;
  }

  public void setActionType(String actionType) {
    this.actionType = actionType;
  }

  public String getComponentPackageUrl() {
    return componentPackageUrl;
  }

  public String getComponentHash() {
    return componentHash;
  }

  public String getVexState() {
    return vexState;
  }

  public void setVexState(String vexState) {
    this.vexState = vexState;
  }
}
