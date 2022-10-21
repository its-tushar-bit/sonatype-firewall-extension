/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.builders;

import java.util.Date;

import com.sonatype.insight.brain.model.policy.PolicyWaiver;

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
}
