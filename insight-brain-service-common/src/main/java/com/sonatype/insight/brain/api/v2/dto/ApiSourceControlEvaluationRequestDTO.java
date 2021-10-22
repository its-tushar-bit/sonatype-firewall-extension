/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

/**
 * @since 1.101
 */
public class ApiSourceControlEvaluationRequestDTO
{
  public String stageId;

  public String branchName;

  /**
   * @since 1.126
   */
  public List<String> scanTargets;

  // For JSON de-serialization
  public ApiSourceControlEvaluationRequestDTO() {
  }

  public ApiSourceControlEvaluationRequestDTO(String stageId, String branchName) {
    this(stageId, branchName, null /* scanTarget */);
  }

  public ApiSourceControlEvaluationRequestDTO(String stageId, String branchName, List<String> scanTargets) {
    this.stageId = stageId;
    this.branchName = branchName;
    this.scanTargets = scanTargets;
  }
}
