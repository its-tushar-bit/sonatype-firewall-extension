/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiCrossStageViolationDTOV2
    extends ApiPolicyViolationDTOV2
{
  public String applicationPublicId;

  public String applicationName;

  public String organizationName;

  public long openTime;

  public Long fixTime;

  public String hash;

  public String policyThreatCategory;

  public ComponentDisplayName displayName;

  // keyed by stageTypeId
  public Map<String, StageData> stageData;

  public PolicyOwner policyOwner;

  public static class StageData
  {
    public long mostRecentEvaluationTime;

    public String mostRecentScanId;
  }

  public static class PolicyOwner
  {
    @JsonInclude(Include.NON_NULL)
    public String ownerPublicId;

    public String ownerId;

    public String ownerName;

    public String ownerType;
  }
}
