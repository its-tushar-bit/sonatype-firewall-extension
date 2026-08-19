/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.utils.CsvWritable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;

/**
 * @since 1.11.0
 */
public class ApplicationRiskScoreDTO
    implements CsvWritable
{
  private static final Joiner joiner = Joiner.on(",");

  /**
   * @since 1.52
   */
  public String organizationName;

  public String applicationName;

  public String applicationId;

  public String organizationId;

  @JsonIgnore
  public String id;

  public RiskDTO totalApplicationRisk = new RiskDTO();

  public List<StageRiskScoreDTO> stageRisks = new ArrayList<>();

  /**
   * Epoch millis of the most recent stage evaluation. Populated for Martha list cards; also
   * serialized on Classic Dashboard responses for apps with violations (additive field).
   */
  public Long lastEvaluationTime;

  public ApplicationRiskScoreDTO() {
  }

  public ApplicationRiskScoreDTO(
      final String organizationName,
      final String organizationId,
      final String applicationName,
      final String applicationId,
      final String id)
  {
    this.organizationName = organizationName;
    this.organizationId = organizationId;
    this.applicationName = applicationName;
    this.applicationId = applicationId;
    this.id = id;
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
    return "Organization Name,Application Name,Total Risk,Critical,Severe,Moderate,Low";
  }

  @Override
  public String toCsvLine() {
    return joiner.join(organizationName, applicationName, totalApplicationRisk.totalRisk,
        totalApplicationRisk.criticalRisk, totalApplicationRisk.severeRisk, totalApplicationRisk.moderateRisk,
        totalApplicationRisk.lowRisk);
  }
}
