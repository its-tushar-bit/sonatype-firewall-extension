/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PolicyViolationLogDTO
{
  public String eventType;

  public String eventTimestamp;

  public String policyViolationId;

  /**
   * When a policy violation is unwaived, it is marked as fixed and a new policy violation is created.
   * The id of the new policy violation is stored in this field.
   */
  public String newPolicyViolationId;

  public String policyId;

  public String policyName;

  public String policyThreatCategory;

  public int policyThreatLevel;

  public String stageTypeId;

  public String stagePolicyAction;

  public String organizationId;

  public String organizationName;

  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

  public String repositoryId;

  public String repositoryPublicId;

  public ComponentIdentifier componentIdentifier;

  public String componentHash;
}
