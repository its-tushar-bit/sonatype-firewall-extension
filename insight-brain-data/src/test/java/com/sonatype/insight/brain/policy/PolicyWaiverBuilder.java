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
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverStatus;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

public class PolicyWaiverBuilder
{
  private final PolicyWaiver policyWaiver;

  public PolicyWaiverBuilder() {
    this.policyWaiver = new PolicyWaiver();
  }

  public PolicyWaiverBuilder setHash(final String hash) {
    policyWaiver.setHash(hash);
    return this;
  }

  public PolicyWaiverBuilder setPolicyId(final String policyId) {
    policyWaiver.setPolicyId(policyId);
    return this;
  }

  public PolicyWaiverBuilder setOwnerId(final String ownerId) {
    policyWaiver.setOwnerId(ownerId);
    return this;
  }

  public PolicyWaiverBuilder setComment(final String comment) {
    policyWaiver.setComment(comment);
    return this;
  }

  public PolicyWaiverBuilder setConstraintFacts(int count) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and");
      ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
          0 /* conditionIndex */, "some summary", "some reason");
      conditionFact.setTriggerJson("{ \"conditionIndex\": " + i + ", \"trigger\": \"some trigger\" }");
      constraintFact.addConditionFact(conditionFact);
      constraintFacts.add(constraintFact);
    }
    policyWaiver.setConstraintFacts(constraintFacts);
    return this;
  }

  public PolicyWaiverBuilder setConstraintFacts(final List<ConstraintFact> constraintFacts) {
    policyWaiver.setConstraintFacts(constraintFacts);
    return this;
  }

  public PolicyWaiverBuilder setComponentMatchStrategy(final ComponentMatcherStrategyForWaiver matchStrategy) {
    this.policyWaiver.setComponentMatchStrategy(matchStrategy);
    return this;
  }

  public PolicyWaiverBuilder setAssociatedPackagedUrl(final String associatedPackagedUrl) {
    this.policyWaiver.setAssociatedPackageUrl(associatedPackagedUrl);
    return this;
  }

  public PolicyWaiverBuilder setStatus(final PolicyWaiverStatus status) {
    policyWaiver.setStatus(status);
    return this;
  }

  public PolicyWaiverBuilder setCreateTime(final Date createTime) {
    policyWaiver.setCreateTime(createTime);
    return this;
  }

  public PolicyWaiver build() {
    return this.policyWaiver;
  }
}
