/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

public class RiskDTO
{
  public long totalRisk;

  public long criticalRisk;

  public long severeRisk;

  public long moderateRisk;

  public long lowRisk;

  public RiskDTO() {
  }

  public RiskDTO(final long totalRisk, final long criticalRisk, final long severeRisk,
      final long moderateRisk, final long lowRisk)
  {
    this.totalRisk = totalRisk;
    this.criticalRisk = criticalRisk;
    this.severeRisk = severeRisk;
    this.moderateRisk = moderateRisk;
    this.lowRisk = lowRisk;
  }

}
