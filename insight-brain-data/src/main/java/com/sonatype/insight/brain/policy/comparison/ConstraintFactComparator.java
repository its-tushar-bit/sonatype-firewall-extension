/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.comparison;

import java.util.Comparator;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;

class ConstraintFactComparator
    implements Comparator<ConstraintFact>
{
  static final Comparator<List<ConditionFact>> CONDITION_FACTS_LIST_COMPARATOR = new ConditionFactsListComparator();

  @Override
  public int compare(ConstraintFact constraintFact1, ConstraintFact constraintFact2) {
    // Constraint id
    int result = constraintFact1.getConstraintId().compareTo(constraintFact2.getConstraintId());
    if (result != 0) {
      return result;
    }

    // Condition facts
    result = CONDITION_FACTS_LIST_COMPARATOR.compare(constraintFact1.getConditionFacts(),
        constraintFact2.getConditionFacts());
    if (result != 0) {
      return result;
    }

    return 0;
  }
}
