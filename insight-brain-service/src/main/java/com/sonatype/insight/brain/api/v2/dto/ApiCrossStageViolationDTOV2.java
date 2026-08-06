/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ApiCrossStageViolationDTOV2
    extends ApiPolicyViolationDTOV2
{
  @JsonInclude(Include.NON_NULL)
  public String applicationPublicId;

  @JsonInclude(Include.NON_NULL)
  public String applicationName;

  public String organizationName;

  @JsonInclude(Include.NON_NULL)
  public String hrcId;

  public String hash;

  public String policyThreatCategory;

  public ComponentDisplayName displayName;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String filename;

  // keyed by stageTypeId
  public Map<String, StageData> stageData;

  public PolicyOwner policyOwner;

  public ReachabilityStatus reachabilityStatus;

  public static class StageData
  {
    @JsonSerialize(using = ISODateSerializer.class)
    public Date mostRecentEvaluationTime;

    public String mostRecentScanId;

    public String actionTypeId;
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
