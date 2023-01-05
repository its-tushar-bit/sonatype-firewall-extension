/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.builders;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

public class TestPolicyBuilder
{
  private final Policy policy;

  public TestPolicyBuilder() {
    this.policy = new Policy();
  }

  public Policy build() {
    return this.policy;
  }

  public TestPolicyBuilder withSampleTestValues() {

    this.policy.setId(null);
    this.policy.setThreatLevel(RandomUtils.nextInt(2, 11));
    this.policy.setName(RandomStringUtils.randomAlphabetic(10));

    // add default constraint
    Constraint constraint = new Constraint(null, RandomStringUtils.randomAlphabetic(10), LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    this.policy.addConstraint(constraint);

    return this;
  }

  public TestPolicyBuilder withOwnerId(String id) {
    this.policy.setOwnerId(id);
    return this;
  }

  public TestPolicyBuilder withName(String name) {
    this.policy.setName(name);
    return this;
  }

  public TestPolicyBuilder withThreatLevel(int threat) {
    this.policy.setThreatLevel(threat);
    return this;
  }
}
