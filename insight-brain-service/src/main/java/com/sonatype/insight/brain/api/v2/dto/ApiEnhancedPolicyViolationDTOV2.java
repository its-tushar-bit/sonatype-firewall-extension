/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

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

  public ApiComponentDTOV2 component;

  public boolean isWaived;

  public boolean isLegacy;
}
