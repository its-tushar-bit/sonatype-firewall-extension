/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

/**
 * Carries data with stage-specific details for some risk entry.
 */
public class StageDetailDTO
{
  public String stageTypeId;

  public String stageTypeName;

  public Long time;

  public String actionTypeId;

  public String scanId;

  public StageDetailDTO() {
  }

  public StageDetailDTO(String stageTypeId, String stageTypeName) {
    this.stageTypeId = stageTypeId;
    this.stageTypeName = stageTypeName;
  }
}
