/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;

public class PolicyThreatCategoryUtil
{
  public static PolicyThreatCategory determinePolicyThreatCategory(List<ConstraintFact> constraintFacts) {
    SortedSet<PolicyThreatCategory> policyThreatCategories = new TreeSet<>();
    for (ConstraintFact constraintFact : constraintFacts) {
      for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
        ConditionType<?> conditionType = ConditionTypes.getById(conditionFact.getConditionTypeId());
        if (conditionType == null) {
          policyThreatCategories.add(PolicyThreatCategory.OTHER);
        }
        else {
          policyThreatCategories.add(conditionType.getThreatCategory());
        }
      }
    }
    return PolicyThreatCategory.getCategory(policyThreatCategories);
  }
}
