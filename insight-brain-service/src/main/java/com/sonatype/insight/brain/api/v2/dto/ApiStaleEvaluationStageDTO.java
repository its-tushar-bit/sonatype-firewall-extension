/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.json.store.ApiDateFormat;

/**
 * @since 1.83
 */
public class ApiStaleEvaluationStageDTO
{
  public String stageId;

  @ApiDateFormat
  public Date lastEvaluationDate;
}
