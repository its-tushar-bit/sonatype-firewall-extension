/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

/**
 * @since 1.11.0
 */
public class ApplicationRiskScoreDTO implements CsvWritable
{
  private static final Joiner joiner = Joiner.on(",");

  public String applicationName;

  public String applicationId;

  public RiskDTO totalApplicationRisk = new RiskDTO();

  public List<StageRiskScoreDTO> stageRisks = new ArrayList<>();

  public ApplicationRiskScoreDTO(final String applicationName,
                                 final String applicationId)
  {
    this.applicationName = applicationName;
    this.applicationId = applicationId;
  }

  public void addStageRiskScore(StageRiskScoreDTO newStageRisk) {
    stageRisks.add(newStageRisk);
  }

  public StageRiskScoreDTO getStageRiskScore(String stageRiskId) {
    for (StageRiskScoreDTO stageRiskDTO : stageRisks) {
      if (stageRiskDTO.stageTypeId.equals(stageRiskId)) {
        return stageRiskDTO;
      }
    }
    return null;
  }

  public static String getCsvHeader() {
    return "Application Name,Total Risk,Critical,Severe,Moderate,Low";
  }

  @Override
  public String toCsvLine() {
    return joiner.join(applicationName, totalApplicationRisk.totalRisk, totalApplicationRisk.criticalRisk,
        totalApplicationRisk.severeRisk, totalApplicationRisk.moderateRisk, totalApplicationRisk.lowRisk);
  }
}
