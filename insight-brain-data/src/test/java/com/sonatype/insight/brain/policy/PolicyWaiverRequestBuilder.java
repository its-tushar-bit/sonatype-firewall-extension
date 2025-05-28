/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

public class PolicyWaiverRequestBuilder
{
  private final PolicyWaiverRequest policyWaiverRequest;

  public PolicyWaiverRequestBuilder() {
    policyWaiverRequest = new PolicyWaiverRequest();
    policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.REQUESTED);
  }

  public PolicyWaiverRequestBuilder setHash(final String hash) {
    policyWaiverRequest.setHash(hash);
    return this;
  }

  public PolicyWaiverRequestBuilder setPolicyId(final String policyId) {
    policyWaiverRequest.setPolicyId(policyId);
    return this;
  }

  public PolicyWaiverRequestBuilder setOwnerId(final String ownerId) {
    policyWaiverRequest.setOwnerId(ownerId);
    return this;
  }

  public PolicyWaiverRequestBuilder setComment(final String comment) {
    policyWaiverRequest.setComment(comment);
    return this;
  }

  public PolicyWaiverRequestBuilder setNoteToReviewer(final String noteToReviewer) {
    policyWaiverRequest.setNoteToReviewer(noteToReviewer);
    return this;
  }

  public PolicyWaiverRequestBuilder setWaiverReasonId(final String waiverReasonId) {
    policyWaiverRequest.setWaiverReasonId(waiverReasonId);
    return this;
  }

  public PolicyWaiverRequestBuilder setConstraintFacts(int count) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and");
      ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
          0 /* conditionIndex */, "some summary", "some reason");
      conditionFact.setTriggerJson("{ \"conditionIndex\": " + i + ", \"trigger\": \"some trigger\" }");
      constraintFact.addConditionFact(conditionFact);
      constraintFacts.add(constraintFact);
    }
    policyWaiverRequest.setConstraintFacts(constraintFacts);
    return this;
  }

  public PolicyWaiverRequestBuilder setConstraintFacts(final List<ConstraintFact> constraintFacts) {
    policyWaiverRequest.setConstraintFacts(constraintFacts);
    return this;
  }

  public PolicyWaiverRequestBuilder setComponentMatchStrategy(final ComponentMatcherStrategyForWaiver matchStrategy) {
    this.policyWaiverRequest.setComponentMatchStrategy(matchStrategy);
    return this;
  }

  public PolicyWaiverRequestBuilder setAssociatedPackageUrl(final String associatedPackageUrl) {
    this.policyWaiverRequest.setAssociatedPackageUrl(associatedPackageUrl);
    return this;
  }

  public PolicyWaiverRequestBuilder setStatus(final PolicyWaiverRequestStatus status) {
    policyWaiverRequest.setStatus(status);
    return this;
  }

  public PolicyWaiverRequestBuilder setRequestTime(final Date createTime) {
    policyWaiverRequest.setRequestTime(createTime);
    return this;
  }

  public PolicyWaiverRequestBuilder setExpiryTime(final Date expiryTime) {
    policyWaiverRequest.setExpiryTime(expiryTime);
    return this;
  }

  public PolicyWaiverRequestBuilder setComponentUpgradeAvailable(final Boolean componentUpgradeAvailable) {
    policyWaiverRequest.setComponentUpgradeAvailable(componentUpgradeAvailable);
    return this;
  }

  public PolicyWaiverRequestBuilder setPolicyWaiverId(final String policyWaiverId) {
    policyWaiverRequest.setPolicyWaiverId(policyWaiverId);
    return this;
  }

  public PolicyWaiverRequestBuilder setPolicyViolationId(final String policyViolationId) {
    policyWaiverRequest.setPolicyViolationId(policyViolationId);
    return this;
  }

  public PolicyWaiverRequest build() {
    return this.policyWaiverRequest;
  }

  public PolicyWaiverRequestBuilder setRequesterId(final String requesterId) {
    policyWaiverRequest.setRequesterId(requesterId);
    return this;
  }

  public PolicyWaiverRequestBuilder setRequesterName(final String requesterName) {
    policyWaiverRequest.setRequesterName(requesterName);
    return this;
  }
}
