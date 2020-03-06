/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;

public class ApiCrossStageViolationDTOV2
    extends ApiPolicyViolationDTOV2
{
  public String applicationPublicId;

  public String applicationName;

  public String organizationName;

  public long openTime;

  public Long fixTime;

  public String hash;

  public ComponentDisplayName displayName;

  // keyed by stageTypeId
  public Map<String, StageData> stageData;

  public static class StageData
  {
    public long mostRecentEvaluationTime;

    public String mostRecentScanId;
  }
}
