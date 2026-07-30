/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyConstraint;
import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @since 1.143
 */
public class ProxyRepositoryPolicyViolationDTO
{
  public String policyViolationId;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public ComponentDisplayName componentDisplayName;

  public String hash;

  public String policyId;

  public String policyName;

  public PolicyOwner policyOwner;

  public int policyThreatLevel;

  public PolicyThreatCategory policyThreatCategory;

  public List<PolicyConstraint> constraints;

  public String constraintFactsJson;

  public boolean waived;

  public String policyActionTypeId;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date lastReported;

  // Needed for de-serialization
  public ProxyRepositoryPolicyViolationDTO() {
  }

  public ProxyRepositoryPolicyViolationDTO(
      String policyViolationId,
      ApiComponentIdentifierDTOV2 componentIdentifier,
      ComponentDisplayName componentDisplayName,
      String hash,
      String policyId,
      String policyName,
      Owner policyOwner,
      int policyThreatLevel,
      PolicyThreatCategory policyThreatCategory,
      List<PolicyThreats.PolicyConstraint> constraints,
      String constraintFactsJson,
      boolean waived,
      String policyActionTypeId,
      Date lastReported)
  {
    this.policyViolationId = policyViolationId;
    this.componentIdentifier = componentIdentifier;
    this.componentDisplayName = componentDisplayName;
    this.hash = hash;
    this.policyId = policyId;
    this.policyName = policyName;
    this.policyOwner = new PolicyOwner();
    if (policyOwner != null) {
      this.policyOwner.ownerId = policyOwner.getId();
      this.policyOwner.ownerName = policyOwner.getName();
      this.policyOwner.ownerType = policyOwner.getType().toString();
    }
    this.policyThreatLevel = policyThreatLevel;
    this.policyThreatCategory = policyThreatCategory;
    this.constraints = constraints;
    this.constraintFactsJson = constraintFactsJson;
    this.waived = waived;
    this.policyActionTypeId = policyActionTypeId;
    this.lastReported = lastReported;
  }

  public static class PolicyOwner
  {
    public String ownerId;

    public String ownerName;

    public String ownerType;
  }
}
