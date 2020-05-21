/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * @since 1.13.0
 */
public class ApiEnhancedPolicyViolationDTOV2
    extends ApiPolicyViolationDTOV2
{
  public String stageId;

  /**
   * @since 1.88
   */
  public String reportId;

  public String reportUrl;

  /**
   * @since 1.91
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  public Date openTime;

  public ApiComponentDTOV2 component;
}
