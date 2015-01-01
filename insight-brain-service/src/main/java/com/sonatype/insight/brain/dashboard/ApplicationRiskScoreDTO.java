/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.organization.ContactDTO;

/**
 * @since 1.11.0
 */
public class ApplicationRiskScoreDTO
{
  public String applicationName;

  public String applicationId;

  public ContactDTO applicationContact;

  public RiskDTO totalApplicationRisk = new RiskDTO();

  public List<StageRiskScoreDTO> stageRisks = new ArrayList<>();

  public ApplicationRiskScoreDTO(final String applicationName, final String applicationId,
      final ContactDTO applicationContact)
  {
    this.applicationName = applicationName;
    this.applicationId = applicationId;
    this.applicationContact = applicationContact;
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
}
