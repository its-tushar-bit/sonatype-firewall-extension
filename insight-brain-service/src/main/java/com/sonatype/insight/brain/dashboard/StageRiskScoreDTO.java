/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

public class StageRiskScoreDTO
{
  public String stageTypeId;

  public String stageTypeName;

  public String scanId;

  /** Epoch millis of the latest policy evaluation for this stage (Martha card date). */
  public Long evaluationTime;

  public RiskDTO risk = new RiskDTO();

  public StageRiskScoreDTO() {
  }

  public StageRiskScoreDTO(final String stageTypeId) {
    this.stageTypeId = stageTypeId;
  }

  public StageRiskScoreDTO(final String stageTypeId, final RiskDTO risk) {
    this.stageTypeId = stageTypeId;
    this.risk = risk;
  }
}
