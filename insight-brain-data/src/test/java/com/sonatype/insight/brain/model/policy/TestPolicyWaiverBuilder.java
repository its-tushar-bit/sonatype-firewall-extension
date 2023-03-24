/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;

public class TestPolicyWaiverBuilder
{
  private final PolicyWaiver policyWaiver;

  public TestPolicyWaiverBuilder() {
    this.policyWaiver = new PolicyWaiver();
  }

  public PolicyWaiver build() {
    return this.policyWaiver;
  }

  public TestPolicyWaiverBuilder withHash(String hash) {
    this.policyWaiver.setHash(hash);
    return this;
  }

  public TestPolicyWaiverBuilder withPolicyId(String id) {
    this.policyWaiver.setPolicyId(id);
    return this;
  }

  public TestPolicyWaiverBuilder withOwnerId(String id) {
    this.policyWaiver.setOwnerId(id);
    return this;
  }

  public TestPolicyWaiverBuilder withExpiryTime(Date date) {
    this.policyWaiver.setExpiryTime(date);
    return this;
  }

  public TestPolicyWaiverBuilder withConstraintFacts(List<ConstraintFact> constraintFacts) {
    this.policyWaiver.setConstraintFacts(constraintFacts);
    return this;
  }

  public TestPolicyWaiverBuilder withAssociatedPackageUrl(String associatedPackageUrl) {
    this.policyWaiver.setAssociatedPackageUrl(associatedPackageUrl);
    return this;
  }

  public TestPolicyWaiverBuilder withComponentMatcherStrategyForWaiver(
      ComponentMatcherStrategyForWaiver
          componentMatcherStrategyForWaiver)
  {
    this.policyWaiver.setComponentMatchStrategy(componentMatcherStrategyForWaiver);
    return this;
  }

  public TestPolicyWaiverBuilder withComment(String comment) {
    this.policyWaiver.setComment(comment);
    return this;
  }

  public TestPolicyWaiverBuilder withCreateTime(Date date) {
    this.policyWaiver.setCreateTime(date);
    return this;
  }

  public TestPolicyWaiverBuilder withComponentUpgradeAvailable(Boolean componentUpgradeAvailable) {
    this.policyWaiver.setComponentUpgradeAvailable(componentUpgradeAvailable);
    return this;
  }

  public TestPolicyWaiverBuilder withCreatorId(String creatorId) {
    this.policyWaiver.setCreatorId(creatorId);
    return this;
  }

  public TestPolicyWaiverBuilder withCreatorName(String creatorName) {
    this.policyWaiver.setCreatorName(creatorName);
    return this;
  }
}
