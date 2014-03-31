/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PolicyTest
{
  @Test
  public void testGetThreatCategory_Security() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint(null, "Constraint2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "1"));
    policy.addConstraint(constraint2);
    Constraint constraint3 = new Constraint(null, "Constraint3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    policy.addConstraint(constraint3);
    Constraint constraint4 = new Constraint(null, "Constraint4", LogicalOperator.AND);
    constraint4.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint4);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.SECURITY));
  }

  @Test
  public void testGetThreatCategory_License() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint(null, "Constraint2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "1"));
    policy.addConstraint(constraint2);
    Constraint constraint3 = new Constraint(null, "Constraint3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    policy.addConstraint(constraint3);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.LICENSE));
  }

  @Test
  public void testGetThreatCategory_Quality_Age() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint(null, "Constraint2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "1"));
    policy.addConstraint(constraint2);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.QUALITY));
  }

  @Test
  public void testGetThreatCategory_Quality_Popularity() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint(null, "Constraint2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(RelativePopularityConditionType.ID, "=", "10"));
    policy.addConstraint(constraint2);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.QUALITY));
  }

  @Test
  public void testGetThreatCategory_Other() {
    Policy policy = new Policy(null, "PolicyTest");
    Constraint constraint1 = new Constraint(null, "Constraint1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    policy.addConstraint(constraint1);

    assertThat(policy.getThreatCategory(), is(PolicyThreatCategory.OTHER));
  }
}
