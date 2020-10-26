/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * @since 1.101
 */
public class ApiManifestEvaluationRequestDTO
{
  public String stageId;

  public String branchName;

  // For JSON de-serialization
  public ApiManifestEvaluationRequestDTO() {
  }

  public ApiManifestEvaluationRequestDTO(String stageId, String branchName) {
    this.stageId = stageId;
    this.branchName = branchName;
  }
}
