/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.json.store.ApiDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

public class ApiPolicyWaiverRequestDTO
{
  @JsonInclude(Include.NON_EMPTY)
  public String policyWaiverRequestId;

  @JsonInclude(Include.NON_EMPTY)
  public String policyViolationId;

  public String comment;

  @JsonInclude(Include.NON_EMPTY)
  public String noteToReviewer;

  @JsonInclude(Include.NON_EMPTY)
  @ApiDateFormat
  public Date requestTime;

  @JsonInclude(Include.NON_EMPTY)
  @ApiDateFormat
  public Date expiryTime;

  @JsonInclude(Include.NON_NULL)
  public Boolean isObsolete;

  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerType;

  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerId;

  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerName;

  public String hash;

  public String policyId;

  @JsonInclude(Include.NON_NULL)
  public String policyName;

  @JsonInclude(Include.NON_EMPTY)
  public String vulnerabilityId;

  @JsonInclude(Include.NON_NULL)
  public List<ConstraintFact> constraintFacts;

  @JsonInclude(Include.NON_NULL)
  public String constraintFactsJson;

  @JsonInclude(Include.NON_NULL)
  public String componentName;

  public String requesterId;

  public String requesterName;

  @JsonInclude(Include.NON_NULL)
  public String reviewerName;

  @JsonInclude(Include.NON_NULL)
  public String reviewerId;

  public ComponentMatcherStrategyForWaiver matcherStrategy;

  public String associatedPackageUrl;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  @JsonInclude(Include.NON_NULL)
  public Integer threatLevel;

  @JsonInclude(Include.NON_NULL)
  public String rejectionReason;

  @JsonProperty(access = Access.READ_ONLY)
  public ComponentDisplayName getDisplayName() {
    return this.componentIdentifier == null
        ? null
        : ComponentDisplayNameUtil.fromIdentifier(this.componentIdentifier.toComponentIdentifier());
  }

  @JsonInclude(Include.NON_NULL)
  public Boolean componentUpgradeAvailable;

  public String reasonText;

  public boolean expireWhenRemediationAvailable;

  public String policyWaiverReasonId;

  public String status;

  /**
   * Whether the current caller may approve/reject this waiver request. Computed server-side so
   * the UI can gate the Approve/Reject buttons correctly even when the request is scoped to a
   * virtual owner (e.g. REPOSITORY_CONTAINER_ID) that the caller lacks direct permission on but
   * whose underlying repository the caller can actually waive on.
   */
  @JsonInclude(Include.NON_NULL)
  public Boolean canReview;
}
