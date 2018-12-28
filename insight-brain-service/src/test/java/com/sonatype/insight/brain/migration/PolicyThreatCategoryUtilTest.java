/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyThreatCategoryUtilTest
{
  @Test
  public void testDeterminePolicyThreatCategory_NoConstraintFacts() {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    PolicyThreatCategory category = PolicyThreatCategoryUtil.determinePolicyThreatCategory(constraintFacts);
    assertThat(category).isEqualTo(PolicyThreatCategory.OTHER);
  }

  @Test
  public void testDeterminePolicyThreatCategory_ConstraintFacts() {
    ConstraintFact constraintFact1 = new ConstraintFact("constraintId1", "constraintName1", "operatorName1");
    constraintFact1
        .addConditionFact(new ConditionFact(MatchStateConditionType.ID, 0 /* conditionIndex */, "summary", "reason"));
    constraintFact1
        .addConditionFact(new ConditionFact(AgeInDaysConditionType.ID, 1 /* conditionIndex */, "summary", "reason"));
    ConstraintFact constraintFact2 = new ConstraintFact("constraintId2", "constraintName2", "operatorName2");
    constraintFact2
        .addConditionFact(new ConditionFact(LabelConditionType.ID, 0 /* conditionIndex */, "summary", "reason"));
    constraintFact2
        .addConditionFact(new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 1 /* conditionIndex */,
            "summary", "reason"));
    List<ConstraintFact> constraintFacts = Arrays.asList(constraintFact1, constraintFact2);
    PolicyThreatCategory category = PolicyThreatCategoryUtil.determinePolicyThreatCategory(constraintFacts);
    assertThat(category).isEqualTo(PolicyThreatCategory.SECURITY);
  }

  @Test
  public void testDeterminePolicyThreatCategory_UnknownConditionType() {
    ConstraintFact constraintFact = new ConstraintFact("constraintId1", "constraintName1", "operatorName1");
    constraintFact
        .addConditionFact(new ConditionFact("Invalid condition type id", 0 /* conditionIndex */, "summary", "reason"));
    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact);
    PolicyThreatCategory category = PolicyThreatCategoryUtil.determinePolicyThreatCategory(constraintFacts);
    assertThat(category).isEqualTo(PolicyThreatCategory.OTHER);
  }
}
