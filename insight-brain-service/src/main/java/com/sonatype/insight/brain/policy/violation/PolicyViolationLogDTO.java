/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.60
 */
@JsonInclude(Include.NON_EMPTY)
public class PolicyViolationLogDTO
{
  public String eventType;

  public String eventTimestamp;

  public String policyId;

  public String policyName;

  public String policyThreatCategory;

  public Integer policyThreatLevel;

  public List<PolicyConditionTriggerDTO> policyConditionTriggers;

  public String stageTypeId;

  public String stagePolicyAction;

  public String userName;

  public String organizationId;

  public String organizationName;

  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

  public String repositoryId;

  public String repositoryPublicId;

  public String repositoryManagerId;

  public String repositoryManagerInstanceId;

  public String repositoryManagerName;

  public ComponentIdentifier componentIdentifier;

  public String componentHash;

  public String tenant;
}
