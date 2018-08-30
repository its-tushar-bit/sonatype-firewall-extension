/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * @since 1.51.0
 */
public class ApiPromoteScanRequestDTOV2
{
  public String scanId;

  public String sourceStageId;

  public String targetStageId;

  public static ApiPromoteScanRequestDTOV2 fromScan(final String scanId, final String targetStageId) {
    ApiPromoteScanRequestDTOV2 apiPromoteScanRequestDTOV2 = new ApiPromoteScanRequestDTOV2();
    apiPromoteScanRequestDTOV2.scanId = scanId;
    apiPromoteScanRequestDTOV2.targetStageId = targetStageId;
    return apiPromoteScanRequestDTOV2;
  }

  public static ApiPromoteScanRequestDTOV2 fromStage(final String sourceStageId, final String targetStageId) {
    ApiPromoteScanRequestDTOV2 apiPromoteScanRequestDTOV2 = new ApiPromoteScanRequestDTOV2();
    apiPromoteScanRequestDTOV2.sourceStageId = sourceStageId;
    apiPromoteScanRequestDTOV2.targetStageId = targetStageId;
    return apiPromoteScanRequestDTOV2;
  }
}
