/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

public class RiskDTO
{
  public int totalRisk;

  public int criticalRisk;

  public int severeRisk;

  public int moderateRisk;

  public int lowRisk;

  public RiskDTO() {
  }

  public RiskDTO(
      final int totalRisk,
      final int criticalRisk,
      final int severeRisk,
      final int moderateRisk,
      final int lowRisk)
  {
    this.totalRisk = totalRisk;
    this.criticalRisk = criticalRisk;
    this.severeRisk = severeRisk;
    this.moderateRisk = moderateRisk;
    this.lowRisk = lowRisk;
  }
}
