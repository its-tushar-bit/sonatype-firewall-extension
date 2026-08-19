/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;

/**
 * Data Transfer Object for multipstep policy evaluation requests.
 * This class encapsulates the data required for evaluating a policy, including
 * the vulnerability signature analysis details.
 */

public class PolicyEvaluationRequestDTO
{
  private VulnerabilitySignatureAnalysisDTO analysisDTO;

  /**
   * Gets the vulnerability signature analysis details.
   *
   * @return the vulnerability signature analysis details
   */
  public VulnerabilitySignatureAnalysisDTO getAnalysisDTO() {
    return analysisDTO;
  }

  /**
   * Sets the vulnerability signature analysis details.
   *
   * @param analysisDTO the vulnerability signature analysis details to set
   */
  public void setAnalysisDTO(VulnerabilitySignatureAnalysisDTO analysisDTO) {
    this.analysisDTO = analysisDTO;
  }
}
